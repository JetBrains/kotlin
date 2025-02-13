/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.transformers.contracts

import org.jetbrains.kotlin.KtFakeSourceElementKind
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.contracts.FirLegacyRawContractDescription
import org.jetbrains.kotlin.fir.contracts.FirRawContractDescription
import org.jetbrains.kotlin.fir.contracts.builder.buildErrorContractDescription
import org.jetbrains.kotlin.fir.contracts.builder.buildLegacyRawContractDescription
import org.jetbrains.kotlin.fir.contracts.builder.buildResolvedContractDescription
import org.jetbrains.kotlin.fir.contracts.description.ConeEffectDeclaration
import org.jetbrains.kotlin.fir.contracts.toFirElement
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.builder.buildAnonymousFunction
import org.jetbrains.kotlin.fir.declarations.builder.buildReceiverParameter
import org.jetbrains.kotlin.fir.declarations.synthetic.FirSyntheticProperty
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.expressions.builder.buildAnonymousFunctionExpression
import org.jetbrains.kotlin.fir.expressions.builder.buildArgumentList
import org.jetbrains.kotlin.fir.expressions.builder.buildBlock
import org.jetbrains.kotlin.fir.expressions.builder.buildFunctionCall
import org.jetbrains.kotlin.fir.expressions.impl.FirContractCallBlock
import org.jetbrains.kotlin.fir.moduleData
import org.jetbrains.kotlin.fir.references.builder.buildSimpleNamedReference
import org.jetbrains.kotlin.fir.resolve.ResolutionMode
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.resolve.transformers.ReturnTypeCalculatorForFullBodyResolve
import org.jetbrains.kotlin.fir.resolve.transformers.body.resolve.BodyResolveContext
import org.jetbrains.kotlin.fir.resolve.transformers.body.resolve.FirAbstractBodyResolveTransformerDispatcher
import org.jetbrains.kotlin.fir.resolve.transformers.body.resolve.FirDeclarationsResolveTransformer
import org.jetbrains.kotlin.fir.resolve.transformers.body.resolve.FirExpressionsResolveTransformer
import org.jetbrains.kotlin.fir.symbols.impl.FirAnonymousFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirReceiverParameterSymbol
import org.jetbrains.kotlin.fir.types.impl.FirImplicitTypeRefImplWithoutSource
import org.jetbrains.kotlin.fir.visitors.transformSingle
import org.jetbrains.kotlin.name.Name

abstract class FirAbstractContractResolveTransformerDispatcher(
    session: FirSession,
    scopeSession: ScopeSession,
    outerBodyResolveContext: BodyResolveContext? = null,
) : FirAbstractBodyResolveTransformerDispatcher(
    session,
    FirResolvePhase.CONTRACTS,
    implicitTypeOnly = false,
    scopeSession,
    returnTypeCalculator = ReturnTypeCalculatorForFullBodyResolve.Contract,
    outerBodyResolveContext = outerBodyResolveContext,
    expandTypeAliases = true,
) {
    final override val expressionsTransformer: FirExpressionsResolveTransformer =
        FirExpressionsResolveTransformer(this)

    final override val declarationsTransformer: FirDeclarationsResolveTransformer
        get() = if (contractMode) contractDeclarationsTransformer else regularDeclarationsTransformer

    protected abstract val contractDeclarationsTransformer: FirDeclarationsContractResolveTransformer
    private val regularDeclarationsTransformer = FirDeclarationsResolveTransformer(this)

    private var contractMode = true
    private var insideContractDescription = false

    override fun transformAnnotation(annotation: FirAnnotation, data: ResolutionMode): FirStatement {
        // Annotations within contracts will be resolved explicitly during BODY_RESOLVE.
        return annotation
    }

    override fun transformAnnotationCall(annotationCall: FirAnnotationCall, data: ResolutionMode): FirStatement {
        // Annotations within contracts will be resolved explicitly during BODY_RESOLVE.
        return annotationCall
    }

    protected open inner class FirDeclarationsContractResolveTransformer :
        FirDeclarationsResolveTransformer(this@FirAbstractContractResolveTransformerDispatcher) {
        override fun transformSimpleFunction(
            simpleFunction: FirSimpleFunction,
            data: ResolutionMode
        ): FirSimpleFunction {
            if (!simpleFunction.hasContractToResolve) return simpleFunction

            return context.withSimpleFunction(simpleFunction, session) {
                context.forFunctionBody(simpleFunction, components) {
                    transformContractDescriptionOwner(simpleFunction)
                }
            }
        }

        override fun transformAnonymousFunction(anonymousFunction: FirAnonymousFunction, data: ResolutionMode): FirAnonymousFunction {
            if (!anonymousFunction.hasContractToResolve) return anonymousFunction

            return context.forFunctionBody(anonymousFunction, components) {
                transformContractDescriptionOwner(anonymousFunction)
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

        override fun transformErrorProperty(errorProperty: FirErrorProperty, data: ResolutionMode): FirStatement {
            return transformProperty(errorProperty, data)
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

        private fun <T : FirContractDescriptionOwner> transformContractDescriptionOwner(owner: T): T {
            dataFlowAnalyzer.enterContractDescription()

            return withEnteringContractDescription {
                when (val contractDescription = owner.contractDescription) {
                    is FirLegacyRawContractDescription ->
                        transformLegacyRawContractDescriptionOwner(owner, contractDescription, hasBodyContract = true)
                    is FirRawContractDescription ->
                        transformRawContractDescriptionOwner(owner, contractDescription)
                    else ->
                        throw IllegalArgumentException("$owner has a contract description of an unknown type")
                }
            }
        }

        private fun <T : FirContractDescriptionOwner> transformLegacyRawContractDescriptionOwner(
            owner: T,
            contractDescription: FirLegacyRawContractDescription,
            hasBodyContract: Boolean
        ): T {
            val valueParameters = owner.valueParameters
            for (valueParameter in valueParameters) {
                context.storeVariable(valueParameter, session)
            }

            val resolvedContractCall = withContractModeDisabled {
                contractDescription.contractCall
                    .transformSingle(transformer, ResolutionMode.ContextIndependent)
                    .apply { replaceConeTypeOrNull(session.builtinTypes.unitType.coneType) }
            }

            // We generate a FirContractCallBlock according to a heuristic, which can have false positives,
            // such as user-defined functions called "contract". In this case, we restore the contract call block
            // to a normal call.
            if (resolvedContractCall.toResolvedCallableSymbol()?.callableId != FirContractsDslNames.CONTRACT) {
                if (hasBodyContract) {
                    owner.body!!.replaceFirstStatement<FirContractCallBlock> { contractDescription.contractCall }
                }

                owner.replaceContractDescription(newContractDescription = null)
                dataFlowAnalyzer.exitContractDescription()
                return owner
            }

            val argument = resolvedContractCall.arguments.singleOrNull() as? FirAnonymousFunctionExpression
                ?: return transformOwnerOfErrorContract(owner, contractDescription, hasBodyContract)

            if (!argument.anonymousFunction.isLambda) {
                return transformOwnerOfErrorContract(owner, contractDescription, hasBodyContract)
            }

            val lambdaBody = argument.anonymousFunction.body
                ?: return transformOwnerOfErrorContract(owner, contractDescription, hasBodyContract)

            val resolvedContractDescription = buildResolvedContractDescription {
                val effectExtractor = ConeEffectExtractor(session, owner, valueParameters)
                for (statement in lambdaBody.statements) {
                    if (statement.source?.kind is KtFakeSourceElementKind.ImplicitReturn) continue
                    when (val effect = statement.accept(effectExtractor, null)) {
                        is ConeEffectDeclaration -> when (effect.erroneous) {
                            false -> effects += effect.toFirElement(statement.source)
                            true -> unresolvedEffects += effect.toFirElement(statement.source)
                        }
                        else -> unresolvedEffects += effect.toFirElement(statement.source)
                    }
                }
                this.source = contractDescription.source
                this.diagnostic = contractDescription.diagnostic
            }
            owner.replaceContractDescription(resolvedContractDescription)
            dataFlowAnalyzer.exitContractDescription()
            return owner
        }

        private inline fun <T> withContractModeDisabled(block: () -> T): T {
            try {
                contractMode = false
                return block()
            } finally {
                contractMode = true
            }
        }

        private inline fun <T> withEnteringContractDescription(block: () -> T): T {
            try {
                insideContractDescription = true
                return block()
            } finally {
                insideContractDescription = false
            }
        }

        private fun <T : FirContractDescriptionOwner> transformRawContractDescriptionOwner(
            owner: T,
            contractDescription: FirRawContractDescription
        ): T {
            val effectsBlock = buildAnonymousFunction {
                moduleData = session.moduleData
                origin = FirDeclarationOrigin.Source
                returnTypeRef = FirImplicitTypeRefImplWithoutSource
                symbol = FirAnonymousFunctionSymbol()
                receiverParameter = buildReceiverParameter {
                    typeRef = FirImplicitTypeRefImplWithoutSource
                    symbol = FirReceiverParameterSymbol()
                    moduleData = session.moduleData
                    origin = FirDeclarationOrigin.Source
                    containingDeclarationSymbol = this@buildAnonymousFunction.symbol
                }
                isLambda = true
                hasExplicitParameterList = true

                body = buildBlock {
                    contractDescription.rawEffects.forEach {
                        statements += it
                    }
                }
            }

            val lambdaArgument = buildAnonymousFunctionExpression {
                anonymousFunction = effectsBlock
                isTrailingLambda = true
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
                this.source = contractDescription.source
            }

            owner.replaceContractDescription(legacyRawContractDescription)

            return transformLegacyRawContractDescriptionOwner(owner, legacyRawContractDescription, hasBodyContract = false)
        }

        open fun transformDeclarationContent(firClass: FirClass, data: ResolutionMode) {
            firClass.transformDeclarations(this, data)
        }

        override fun withFile(file: FirFile, action: () -> FirFile): FirFile {
            return context.withFile(file, components) {
                action()
            }
        }

        override fun transformRegularClass(regularClass: FirRegularClass, data: ResolutionMode): FirRegularClass {
            return withRegularClass(regularClass) {
                transformDeclarationContent(regularClass, data)
                regularClass
            }
        }

        override fun withRegularClass(regularClass: FirRegularClass, action: () -> FirRegularClass): FirRegularClass {
            return context.withRegularClass(regularClass, components) {
                action()
            }
        }

        override fun withScript(script: FirScript, action: () -> FirScript): FirScript {
            return context.withScript(script, components) {
                action()
            }
        }

        override fun transformScript(script: FirScript, data: ResolutionMode): FirScript {
            return withScript(script) {
                transformDeclarationContent(script, data) as FirScript
            }
        }

        override fun transformReplSnippet(replSnippet: FirReplSnippet, data: ResolutionMode): FirReplSnippet {
            return replSnippet
        }

        override fun transformAnonymousObject(
            anonymousObject: FirAnonymousObject,
            data: ResolutionMode
        ): FirAnonymousObject {
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
            if (!constructor.hasContractToResolve) {
                return constructor
            }
            return context.withConstructor(constructor) {
                context.forConstructorBody(constructor, session) {
                    transformContractDescriptionOwner(constructor)
                }
            }
        }

        override fun transformTypeAlias(typeAlias: FirTypeAlias, data: ResolutionMode): FirTypeAlias {
            return typeAlias
        }

        override fun transformDanglingModifierList(
            danglingModifierList: FirDanglingModifierList,
            data: ResolutionMode
        ): FirDanglingModifierList {
            return danglingModifierList
        }

        override fun transformErrorPrimaryConstructor(
            errorPrimaryConstructor: FirErrorPrimaryConstructor,
            data: ResolutionMode,
        ): FirErrorPrimaryConstructor = transformConstructor(errorPrimaryConstructor, data) as FirErrorPrimaryConstructor

        override fun transformEnumEntry(enumEntry: FirEnumEntry, data: ResolutionMode): FirEnumEntry {
            return enumEntry
        }

        private fun <T : FirContractDescriptionOwner> transformOwnerOfErrorContract(
            owner: T,
            description: FirLegacyRawContractDescription,
            hasBodyContract: Boolean,
        ): T {
            owner.replaceContractDescription(
                buildErrorContractDescription {
                    source = description.source
                    diagnostic = description.diagnostic
                }
            )

            // Error contract should be unwrapped to properly resolve it later
            if (hasBodyContract) {
                owner.body!!.replaceFirstStatement<FirContractCallBlock> { description.contractCall }
            }

            dataFlowAnalyzer.exitContractDescription()
            return owner
        }

        private val FirContractDescriptionOwner.hasContractToResolve: Boolean
            get() = contractDescription is FirLegacyRawContractDescription || contractDescription is FirRawContractDescription
    }
}
