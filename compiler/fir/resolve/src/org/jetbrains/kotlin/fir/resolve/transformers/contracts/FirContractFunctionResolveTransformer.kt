/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.transformers.contracts

import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.contracts.*
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.render
import org.jetbrains.kotlin.fir.resolve.ResolutionMode
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.resolve.transformers.body.resolve.FirBodyResolveTransformer
import org.jetbrains.kotlin.fir.resolve.transformers.body.resolve.FirDeclarationsResolveTransformer
import org.jetbrains.kotlin.fir.resolve.transformers.obtainResolvedContractDescription
import org.jetbrains.kotlin.fir.resolve.transformers.wrapEffectsInContractCall
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.visitors.*

class FirContractFunctionResolveTransformer(
    session: FirSession,
    scopeSession: ScopeSession,
    outerBodyResolveContext: BodyResolveContext? = null
) : FirBodyResolveTransformer(
    session,
    FirResolvePhase.CONTRACT_FUNCTIONS,
    implicitTypeOnly = false,
    scopeSession,
    outerBodyResolveContext = outerBodyResolveContext
) {
    override val declarationsTransformer: FirDeclarationsResolveTransformer = FirDeclarationsContractFunctionResolveTransformer(this)

    private class FirDeclarationsContractFunctionResolveTransformer(transformer: FirBodyResolveTransformer) : FirDeclarationsResolveTransformer(transformer) {
        private val contractFunctionResolveSession = ContractFunctionResolveSession()

        override fun <E : FirElement> transformElement(element: E, data: ResolutionMode): CompositeTransformResult<E> {
            return element.compose()
        }

        override fun transformContractFunction(
            contractFunction: FirContractFunction,
            data: ResolutionMode
        ): CompositeTransformResult<FirDeclaration> {
            val symbol = contractFunction.symbol
            val status = contractFunctionResolveSession.getStatus(symbol)
            if (status is ContractFunctionResolveComputationStatus.Computed) {
                return status.transformedContractFunction.compose()
            }

            // If the given contract function is currently being computed then a loop has been found
            require(status is ContractFunctionResolveComputationStatus.NotComputed) {
                "Unexpected status in transformContractFunction ($status) for ${contractFunction.render()}"
            }

            contractFunctionResolveSession.startComputing(symbol)

            contractFunction.updatePhase()
            dataFlowAnalyzer.enterContractDescription()

            val contractDescription = contractFunction.contractDescription as? FirRawContractDescription
                ?: return transformErrorContractFunction(contractFunction)
            val result = doTransformContractFunction(contractFunction, contractDescription)

            dataFlowAnalyzer.exitContractDescription()
            contractFunctionResolveSession.storeResult(symbol, result.single)
            return result
        }

        private fun doTransformContractFunction(
            contractFunction: FirContractFunction,
            contractDescription: FirRawContractDescription
        ): CompositeTransformResult<FirContractFunction> {
            wrapEffectsInContractCall(session, contractFunction, contractDescription)
            val legacyRawContractDescription = contractFunction.contractDescription as? FirLegacyRawContractDescription
                ?: return transformErrorContractFunction(contractFunction)
            return transformLegacyRawContractDescription(contractFunction, legacyRawContractDescription)
        }

        private fun transformLegacyRawContractDescription(
            contractFunction: FirContractFunction,
            contractDescription: FirLegacyRawContractDescription
        ): CompositeTransformResult<FirContractFunction> {
            val valueParameters = contractFunction.valueParameters
            val contractCall = withNewLocalScope {
                for (valueParameter in valueParameters) {
                    context.storeVariable(valueParameter)
                }
                context.withContainer(contractFunction as FirDeclaration) {
                    contractDescription.contractCall.transformSingle(transformer, ResolutionMode.ContextIndependent)
                }
            }

            val argument = contractCall.argument as? FirLambdaArgumentExpression ?: return transformErrorContractFunction(contractFunction)
            val lambdaBody = (argument.expression as FirAnonymousFunction).body ?: return transformErrorContractFunction(contractFunction)

            val resolvedContractDescription = obtainResolvedContractDescription(
                session,
                transformer,
                contractFunction,
                valueParameters,
                lambdaBody
            )
            contractFunction.replaceContractDescription(resolvedContractDescription)

            return contractFunction.compose()
        }

        private fun transformErrorContractFunction(contractFunction: FirContractFunction): CompositeTransformResult<FirContractFunction> {
            // TODO
            dataFlowAnalyzer.exitContractDescription()
            return contractFunction.compose()
        }

        override fun transformDeclarationStatus(
            declarationStatus: FirDeclarationStatus,
            data: ResolutionMode
        ): CompositeTransformResult<FirDeclarationStatus> {
            return transformElement(declarationStatus, data)
        }

        override fun transformProperty(property: FirProperty, data: ResolutionMode): CompositeTransformResult<FirProperty> {
            return transformElement(property, data)
        }

        override fun transformWrappedDelegateExpression(
            wrappedDelegateExpression: FirWrappedDelegateExpression,
            data: ResolutionMode
        ): CompositeTransformResult<FirStatement> {
            return transformElement(wrappedDelegateExpression, data)
        }

        override fun transformRegularClass(regularClass: FirRegularClass, data: ResolutionMode): CompositeTransformResult<FirStatement> {
            return transformElement(regularClass, data)
        }

        override fun transformAnonymousObject(
            anonymousObject: FirAnonymousObject,
            data: ResolutionMode
        ): CompositeTransformResult<FirStatement> {
            return transformElement(anonymousObject, data)
        }

        override fun transformSimpleFunction(
            simpleFunction: FirSimpleFunction,
            data: ResolutionMode
        ): CompositeTransformResult<FirSimpleFunction> {
            return transformElement(simpleFunction, data)
        }

        override fun transformConstructor(constructor: FirConstructor, data: ResolutionMode): CompositeTransformResult<FirDeclaration> {
            return transformElement(constructor, data)
        }

        override fun transformAnonymousInitializer(
            anonymousInitializer: FirAnonymousInitializer,
            data: ResolutionMode
        ): CompositeTransformResult<FirDeclaration> {
            return transformElement(anonymousInitializer, data)
        }

        override fun transformValueParameter(
            valueParameter: FirValueParameter,
            data: ResolutionMode
        ): CompositeTransformResult<FirStatement> {
            return transformElement(valueParameter, data)
        }

        private fun FirDeclaration.updatePhase() {
            replaceResolvePhase(FirResolvePhase.CONTRACTS)
        }
    }
}

private class ContractFunctionResolveSession {
    private val contractFunctionResolveStatusMap = hashMapOf<FirCallableSymbol<*>, ContractFunctionResolveComputationStatus>()

    fun getStatus(symbol: FirCallableSymbol<*>): ContractFunctionResolveComputationStatus {
        return contractFunctionResolveStatusMap[symbol] ?: ContractFunctionResolveComputationStatus.NotComputed
    }

    fun startComputing(symbol: FirCallableSymbol<*>) {
        require(contractFunctionResolveStatusMap[symbol] == null) {
            "Unexpected status in startComputing for $symbol: ${contractFunctionResolveStatusMap[symbol]}"
        }

        contractFunctionResolveStatusMap[symbol] = ContractFunctionResolveComputationStatus.Computing
    }

    fun storeResult(
        symbol: FirCallableSymbol<*>,
        transformedContractFunction: FirContractFunction
    ) {
        require(contractFunctionResolveStatusMap[symbol] == ContractFunctionResolveComputationStatus.Computing) {
            "Unexpected status in storeResult for $symbol: ${contractFunctionResolveStatusMap[symbol]}"
        }

        val contractDescription = transformedContractFunction.contractDescription
        require(contractDescription is FirResolvedContractDescription) {
            "Contract description is not a FirResolvedContractDescription for: ${symbol.fir.render()}"
        }

        contractFunctionResolveStatusMap[symbol] = ContractFunctionResolveComputationStatus.Computed(transformedContractFunction)
    }
}

private sealed class ContractFunctionResolveComputationStatus {
    object NotComputed : ContractFunctionResolveComputationStatus()
    object Computing : ContractFunctionResolveComputationStatus()

    class Computed(
        val transformedContractFunction: FirContractFunction
    ) : ContractFunctionResolveComputationStatus()
}