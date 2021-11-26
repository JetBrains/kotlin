/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.transformers.contracts

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.contracts.FirLegacyRawContractDescription
import org.jetbrains.kotlin.fir.contracts.FirRawContractDescription
import org.jetbrains.kotlin.fir.contracts.builder.buildLegacyRawContractDescription
import org.jetbrains.kotlin.fir.contracts.builder.buildResolvedContractDescription
import org.jetbrains.kotlin.fir.contracts.description.ConeEffectDeclaration
import org.jetbrains.kotlin.fir.contracts.impl.FirEmptyContractDescription
import org.jetbrains.kotlin.fir.contracts.toFirEffectDeclaration
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.builder.buildAnonymousFunction
import org.jetbrains.kotlin.fir.declarations.synthetic.FirSyntheticProperty
import org.jetbrains.kotlin.fir.errorTypeFromPrototype
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.expressions.builder.*
import org.jetbrains.kotlin.fir.moduleData
import org.jetbrains.kotlin.fir.references.builder.buildSimpleNamedReference
import org.jetbrains.kotlin.fir.resolve.ResolutionMode
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.resolve.diagnostics.ConeContractDescriptionError
import org.jetbrains.kotlin.fir.resolve.transformers.body.resolve.BodyResolveContext
import org.jetbrains.kotlin.fir.resolve.transformers.body.resolve.FirBodyResolveTransformer
import org.jetbrains.kotlin.fir.resolve.transformers.body.resolve.FirDeclarationsResolveTransformer
import org.jetbrains.kotlin.fir.symbols.impl.FirAnonymousFunctionSymbol
import org.jetbrains.kotlin.fir.types.builder.buildImplicitTypeRef
import org.jetbrains.kotlin.fir.visitors.transformSingle
import org.jetbrains.kotlin.name.Name

open class FirContractResolveTransformer(
    session: FirSession,
    scopeSession: ScopeSession,
    outerBodyResolveContext: BodyResolveContext? = null
) : FirBodyResolveTransformer(
    session,
    FirResolvePhase.CONTRACTS,
    implicitTypeOnly = false,
    scopeSession,
    outerBodyResolveContext = outerBodyResolveContext
) {
    override val declarationsTransformer: FirDeclarationsResolveTransformer = FirDeclarationsContractResolveTransformer(this)

    override fun transformAnnotation(annotation: FirAnnotation, data: ResolutionMode): FirStatement {
        return annotation
    }

    override fun transformAnnotationCall(annotationCall: FirAnnotationCall, data: ResolutionMode): FirStatement {
        return annotationCall
    }

    protected open class FirDeclarationsContractResolveTransformer(transformer: FirBodyResolveTransformer) : FirDeclarationsResolveTransformer(transformer) {
        override fun transformSimpleFunction(
            simpleFunction: FirSimpleFunction,
            data: ResolutionMode
        ): FirSimpleFunction {
            if (!simpleFunction.hasContractToResolve) {
                return simpleFunction
            }
            val containingDeclaration = context.containerIfAny
            if (containingDeclaration != null && containingDeclaration !is FirClass) {
                simpleFunction.replaceReturnTypeRef(
                    simpleFunction.returnTypeRef.errorTypeFromPrototype(
                        ConeContractDescriptionError("Local function can not be used in contract description")
                    )
                )
                return simpleFunction
            }
            @Suppress("UNCHECKED_CAST")
            return context.withSimpleFunction(simpleFunction, session) {
                context.forFunctionBody(simpleFunction, components) {
                    transformContractDescriptionOwner(simpleFunction)
                }
            }
        }

        override fun transformProperty(property: FirProperty, data: ResolutionMode): FirProperty {
            if (
                property.getter?.hasContractToResolve != true && property.setter?.hasContractToResolve != true ||
                property.isLocal || property.delegate != null
            ) {
                return property
            }
            if (property is FirSyntheticProperty) {
                transformSimpleFunction(property.getter.delegate, data)
                return property
            }
            context.withProperty(property) {
                property.getter?.let { transformPropertyAccessor(it, property) }
                property.setter?.let { transformPropertyAccessor(it, property) }
            }
            return property
        }

        override fun transformField(field: FirField, data: ResolutionMode): FirField {
            return field
        }

        private fun transformPropertyAccessor(
            propertyAccessor: FirPropertyAccessor,
            owner: FirProperty
        ): FirStatement {
            if (!propertyAccessor.hasContractToResolve) {
                return propertyAccessor
            }
            return context.withPropertyAccessor(owner, propertyAccessor, components, forContracts = true) {
                transformContractDescriptionOwner(propertyAccessor)
            }
        }

        private fun <T : FirContractDescriptionOwner> transformContractDescriptionOwner(
            owner: T
        ): T {
            dataFlowAnalyzer.enterContractDescription()
            return when (val contractDescription = owner.contractDescription) {
                is FirLegacyRawContractDescription -> transformLegacyRawContractDescriptionOwner(owner, contractDescription)
                is FirRawContractDescription -> transformRawContractDescriptionOwner(owner, contractDescription)
                else -> throw IllegalArgumentException("$owner has a contract description of an unknown type")
            }
        }

        private fun <T : FirContractDescriptionOwner> transformLegacyRawContractDescriptionOwner(
            owner: T,
            contractDescription: FirLegacyRawContractDescription
        ): T {
            val valueParameters = owner.valueParameters
            for (valueParameter in valueParameters) {
                context.storeVariable(valueParameter, session)
            }
            val contractCall = contractDescription.contractCall.transformSingle(transformer, ResolutionMode.ContextIndependent)
            val resolvedId = contractCall.toResolvedCallableSymbol()?.callableId ?: return transformOwnerWithUnresolvedContract(owner)
            if (resolvedId != FirContractsDslNames.CONTRACT) return transformOwnerWithUnresolvedContract(owner)
            if (contractCall.arguments.size != 1) return transformOwnerOfErrorContract(owner)
            val argument = contractCall.argument as? FirLambdaArgumentExpression ?: return transformOwnerOfErrorContract(owner)
            val lambdaBody = (argument.expression as FirAnonymousFunctionExpression).anonymousFunction.body
                ?: return transformOwnerOfErrorContract(owner)

            val resolvedContractDescription = buildResolvedContractDescription {
                val effectExtractor = ConeEffectExtractor(session, owner, valueParameters)
                for (statement in lambdaBody.statements) {
                    val effect = statement.accept(effectExtractor, null) as? ConeEffectDeclaration
                    if (effect == null) {
                        unresolvedEffects += statement
                    } else {
                        effects += effect.toFirEffectDeclaration(statement.source)
                    }
                }
                this.source = owner.contractDescription.source
            }
            owner.replaceContractDescription(resolvedContractDescription)
            dataFlowAnalyzer.exitContractDescription()
            return owner
        }

        private fun <T : FirContractDescriptionOwner> transformRawContractDescriptionOwner(
            owner: T,
            contractDescription: FirRawContractDescription
        ): T {
            val effectsBlock = buildAnonymousFunction {
                moduleData = session.moduleData
                origin = FirDeclarationOrigin.Source
                returnTypeRef = buildImplicitTypeRef()
                receiverTypeRef = buildImplicitTypeRef()
                symbol = FirAnonymousFunctionSymbol()
                isLambda = true
                hasExplicitParameterList = true

                body = buildBlock {
                    contractDescription.rawEffects.forEach {
                        statements += it
                    }
                }
            }

            val lambdaArgument = buildLambdaArgumentExpression {
                expression = buildAnonymousFunctionExpression {
                    anonymousFunction = effectsBlock
                }
            }

            val contractCall = buildFunctionCall {
                calleeReference = buildSimpleNamedReference {
                    name = Name.identifier("contract")
                }
                argumentList = buildArgumentList {
                    arguments += lambdaArgument
                }
            }

            val legacyRawContractDescription = buildLegacyRawContractDescription {
                this.contractCall = contractCall
            }

            owner.replaceContractDescription(legacyRawContractDescription)

            return transformLegacyRawContractDescriptionOwner(owner, legacyRawContractDescription)
        }

        private fun <T : FirContractDescriptionOwner> transformOwnerWithUnresolvedContract(owner: T): T {
            return when (val contractDescription = owner.contractDescription) {
                is FirLegacyRawContractDescription -> { // old syntax contract description
                    val functionCall = contractDescription.contractCall
                    owner.replaceContractDescription(FirEmptyContractDescription)
                    owner.body.replaceFirstStatement(functionCall)
                    dataFlowAnalyzer.exitContractDescription()
                    owner
                }
                is FirRawContractDescription -> { // new syntax contract description
                    owner.replaceContractDescription(FirEmptyContractDescription)
                    dataFlowAnalyzer.exitContractDescription()
                    owner
                }
                else -> owner // TODO: change
            }
        }

        open fun transformDeclarationContent(firClass: FirClass, data: ResolutionMode) {
            firClass.transformDeclarations(this, data)
        }

        override fun transformRegularClass(regularClass: FirRegularClass, data: ResolutionMode): FirStatement {
            context.withRegularClass(regularClass, components, forContracts = true) {
                transformDeclarationContent(regularClass, data)
            }
            return regularClass
        }

        override fun transformAnonymousObject(
            anonymousObject: FirAnonymousObject,
            data: ResolutionMode
        ): FirStatement {
            context.withAnonymousObject(anonymousObject, components) {
                transformDeclarationContent(anonymousObject, data)
            }
            return anonymousObject
        }

        override fun transformAnonymousInitializer(
            anonymousInitializer: FirAnonymousInitializer,
            data: ResolutionMode
        ): FirAnonymousInitializer {
            return anonymousInitializer
        }

        override fun transformConstructor(constructor: FirConstructor, data: ResolutionMode): FirConstructor {
            return constructor
        }

        override fun transformEnumEntry(enumEntry: FirEnumEntry, data: ResolutionMode): FirEnumEntry {
            return enumEntry
        }

        private fun <T : FirContractDescriptionOwner> transformOwnerOfErrorContract(owner: T): T {
            // TODO
            dataFlowAnalyzer.exitContractDescription()
            return owner
        }

        private val FirContractDescriptionOwner.hasContractToResolve: Boolean
            get() = contractDescription is FirLegacyRawContractDescription || contractDescription is FirRawContractDescription
    }
}

private val FirContractDescriptionOwner.valueParameters: List<FirValueParameter>
    get() = when (this) {
        is FirSimpleFunction -> valueParameters
        is FirPropertyAccessor -> valueParameters
        else -> error()
    }

private val FirContractDescriptionOwner.body: FirBlock
    get() = when (this) {
        is FirSimpleFunction -> body!!
        is FirPropertyAccessor -> body!!
        else -> error()
    }

private fun FirContractDescriptionOwner.error(): Nothing = throw IllegalStateException("${this::class} can not be a contract owner")
