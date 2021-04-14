/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.transformers.contracts

import org.jetbrains.kotlin.fir.*
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
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.expressions.builder.*
import org.jetbrains.kotlin.fir.references.builder.buildSimpleNamedReference
import org.jetbrains.kotlin.fir.resolve.ResolutionMode
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.resolve.diagnostics.ConeContractDescriptionError
import org.jetbrains.kotlin.fir.resolve.transformers.body.resolve.BodyResolveContext
import org.jetbrains.kotlin.fir.resolve.transformers.body.resolve.FirBodyResolveTransformer
import org.jetbrains.kotlin.fir.resolve.transformers.body.resolve.FirDeclarationsResolveTransformer
import org.jetbrains.kotlin.fir.symbols.impl.FirAnonymousFunctionSymbol
import org.jetbrains.kotlin.fir.types.builder.buildImplicitTypeRef
import org.jetbrains.kotlin.fir.visitors.*
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

    override fun transformAnnotationCall(annotationCall: FirAnnotationCall, data: ResolutionMode): FirStatement {
        return annotationCall
    }

    private class FirDeclarationsContractResolveTransformer(transformer: FirBodyResolveTransformer) : FirDeclarationsResolveTransformer(transformer) {
        override fun transformSimpleFunction(
            simpleFunction: FirSimpleFunction,
            data: ResolutionMode
        ): FirSimpleFunction {
            simpleFunction.updatePhase()
            if (!simpleFunction.hasContractToResolve) {
                return simpleFunction
            }
            val containingDeclaration = context.containerIfAny
            if (containingDeclaration != null && containingDeclaration !is FirClass<*>) {
                simpleFunction.replaceReturnTypeRef(
                    simpleFunction.returnTypeRef.errorTypeFromPrototype(
                        ConeContractDescriptionError("Local function can not be used in contract description")
                    )
                )
                return simpleFunction
            }
            @Suppress("UNCHECKED_CAST")
            return context.withSimpleFunction(simpleFunction) {
                context.forFunctionBody(simpleFunction, components) {
                    transformContractDescriptionOwner(simpleFunction)
                }
            }
        }

        override fun transformProperty(property: FirProperty, data: ResolutionMode): FirProperty {
            property.updatePhase()
            if (
                property.getter?.hasContractToResolve != true && property.setter?.hasContractToResolve != true ||
                property.isLocal || property.delegate != null
            ) {
                property.updatePhaseForAccessors()
                return property
            }
            if (property is FirSyntheticProperty) {
                transformSimpleFunction(property.getter.delegate, data)
                property.updatePhaseForAccessors()
                return property
            }
            context.withProperty(property) {
                property.getter?.let { transformPropertyAccessor(it, property) }
                property.setter?.let { transformPropertyAccessor(it, property) }
            }
            return property
        }

        private fun FirProperty.updatePhaseForAccessors() {
            getter?.updatePhase()
            setter?.updatePhase()
        }

        override fun transformField(field: FirField, data: ResolutionMode): FirDeclaration {
            field.updatePhase()
            return field
        }

        private fun transformPropertyAccessor(
            propertyAccessor: FirPropertyAccessor,
            owner: FirProperty
        ): FirStatement {
            propertyAccessor.updatePhase()
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
                context.storeVariable(valueParameter)
            }
            val contractCall = contractDescription.contractCall.transformSingle(transformer, ResolutionMode.ContextIndependent)
            val resolvedId = contractCall.toResolvedCallableSymbol()?.callableId ?: return transformOwnerWithUnresolvedContract(owner)
            if (resolvedId != FirContractsDslNames.CONTRACT) return transformOwnerWithUnresolvedContract(owner)
            if (contractCall.arguments.size != 1) return transformOwnerOfErrorContract(owner)
            val argument = contractCall.argument as? FirLambdaArgumentExpression ?: return transformOwnerOfErrorContract(owner)
            val lambdaBody = (argument.expression as FirAnonymousFunction).body ?: return transformOwnerOfErrorContract(owner)

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
                session = this@FirDeclarationsContractResolveTransformer.session
                origin = FirDeclarationOrigin.Source
                returnTypeRef = buildImplicitTypeRef()
                receiverTypeRef = buildImplicitTypeRef()
                symbol = FirAnonymousFunctionSymbol()
                isLambda = true

                body = buildBlock {
                    contractDescription.rawEffects.forEach {
                        statements += it
                    }
                }
            }

            val lambdaArgument = buildLambdaArgumentExpression {
                expression = effectsBlock
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

        override fun transformRegularClass(regularClass: FirRegularClass, data: ResolutionMode): FirStatement {
            regularClass.updatePhase()
            regularClass.transformCompanionObject(this, data)
            context.withRegularClass(regularClass, components, forContracts = true) {
                regularClass.transformDeclarations(this, data)
            }
            return regularClass
        }

        override fun transformAnonymousObject(
            anonymousObject: FirAnonymousObject,
            data: ResolutionMode
        ): FirStatement {
            anonymousObject.updatePhase()
            context.withAnonymousObject(anonymousObject, components) {
                anonymousObject.transformDeclarations(this, data)
            }
            return anonymousObject
        }

        override fun transformAnonymousInitializer(
            anonymousInitializer: FirAnonymousInitializer,
            data: ResolutionMode
        ): FirDeclaration {
            anonymousInitializer.updatePhase()
            return anonymousInitializer
        }

        override fun transformConstructor(constructor: FirConstructor, data: ResolutionMode): FirDeclaration {
            constructor.updatePhase()
            return constructor
        }

        override fun transformEnumEntry(enumEntry: FirEnumEntry, data: ResolutionMode): FirDeclaration {
            enumEntry.updatePhase()
            return enumEntry
        }

        private fun <T : FirContractDescriptionOwner> transformOwnerOfErrorContract(owner: T): T {
            // TODO
            dataFlowAnalyzer.exitContractDescription()
            return owner
        }

        private val FirContractDescriptionOwner.hasContractToResolve: Boolean
            get() = contractDescription is FirLegacyRawContractDescription || contractDescription is FirRawContractDescription

        private fun FirDeclaration.updatePhase() {
            transformer.replaceDeclarationResolvePhaseIfNeeded(this, FirResolvePhase.CONTRACTS)
        }
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
