/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.transformers.contracts

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.contracts.FirRawContractDescription
import org.jetbrains.kotlin.fir.contracts.builder.buildResolvedContractDescription
import org.jetbrains.kotlin.fir.contracts.description.ConeEffectDeclaration
import org.jetbrains.kotlin.fir.contracts.impl.FirEmptyContractDescription
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.errorTypeFromPrototype
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.resolve.ResolutionMode
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.resolve.diagnostics.ConeContractDescriptionError
import org.jetbrains.kotlin.fir.resolve.transformers.body.resolve.FirBodyResolveTransformer
import org.jetbrains.kotlin.fir.resolve.transformers.body.resolve.FirDeclarationsResolveTransformer
import org.jetbrains.kotlin.fir.scopes.impl.FirLocalScope
import org.jetbrains.kotlin.fir.types.coneTypeUnsafe
import org.jetbrains.kotlin.fir.visitors.CompositeTransformResult
import org.jetbrains.kotlin.fir.visitors.compose
import org.jetbrains.kotlin.fir.visitors.transformSingle

class FirContractResolveTransformer(session: FirSession, scopeSession: ScopeSession) : FirBodyResolveTransformer(
    session,
    FirResolvePhase.CONTRACTS,
    implicitTypeOnly = false,
    scopeSession
) {
    override val declarationsTransformer: FirDeclarationsResolveTransformer = FirDeclarationsContractResolveTransformer(this)

    private class FirDeclarationsContractResolveTransformer(transformer: FirBodyResolveTransformer) : FirDeclarationsResolveTransformer(transformer) {
        override fun transformSimpleFunction(
            simpleFunction: FirSimpleFunction,
            data: ResolutionMode
        ): CompositeTransformResult<FirSimpleFunction> {
            if (!simpleFunction.hasContractToResolve) {
                simpleFunction.replaceResolvePhase(FirResolvePhase.CONTRACTS)
                return simpleFunction.compose()
            }
            val containingDeclaration = context.containerIfAny
            if (containingDeclaration != null && containingDeclaration !is FirClass<*>) {
                simpleFunction.replaceResolvePhase(FirResolvePhase.CONTRACTS)
                simpleFunction.replaceReturnTypeRef(
                    simpleFunction.returnTypeRef.errorTypeFromPrototype(
                        ConeContractDescriptionError("Local function can not be used in contract description")
                    )
                )
                return simpleFunction.compose()
            }
            @Suppress("UNCHECKED_CAST")
            return withTypeParametersOf(simpleFunction) {
                val receiverTypeRef = simpleFunction.receiverTypeRef
                if (receiverTypeRef != null) {
                    withLabelAndReceiverType(simpleFunction.name, simpleFunction, receiverTypeRef.coneTypeUnsafe()) {
                        transformContractDescriptionOwner(simpleFunction)
                    }
                } else {
                    transformContractDescriptionOwner(simpleFunction)
                }
            }
        }

        private fun <T : FirContractDescriptionOwner> transformContractDescriptionOwner(
            owner: T
        ): CompositeTransformResult<T> {
            dataFlowAnalyzer.enterContractDescription()
            val contractDescription = owner.contractDescription
            require(contractDescription is FirRawContractDescription)
            val valueParameters = owner.valueParameters
            val contractCall = withLocalScopeCleanup {
                addLocalScope(FirLocalScope())
                for (valueParameter in valueParameters) {
                    context.storeVariable(valueParameter)
                }
                context.withContainer(owner as FirDeclaration) {
                    contractDescription.contractCall.transformSingle(transformer, ResolutionMode.ContextIndependent)
                }
            }
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
                        effects += effect
                    }
                }
            }
            owner.replaceContractDescription(resolvedContractDescription)
            dataFlowAnalyzer.exitContractDescription()
            return owner.compose()
        }

        private fun <T : FirContractDescriptionOwner> transformOwnerWithUnresolvedContract(owner: T): CompositeTransformResult<T> {
            val contractDescription = owner.contractDescription as FirRawContractDescription
            val functionCall = contractDescription.contractCall
            owner.replaceContractDescription(FirEmptyContractDescription)
            owner.body.replaceFirstStatement(functionCall)
            dataFlowAnalyzer.exitContractDescription()
            return owner.compose()
        }

        private fun <T : FirContractDescriptionOwner> transformOwnerOfErrorContract(owner: T): CompositeTransformResult<T> {
            // TODO
            dataFlowAnalyzer.exitContractDescription()
            return owner.compose()
        }

        private val FirContractDescriptionOwner.hasContractToResolve: Boolean
            get() = contractDescription is FirRawContractDescription
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
