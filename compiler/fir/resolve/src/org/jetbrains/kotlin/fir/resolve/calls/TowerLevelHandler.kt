/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.calls

import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.FirResolvedQualifier
import org.jetbrains.kotlin.fir.scopes.ProcessorAction
import org.jetbrains.kotlin.fir.symbols.AbstractFirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.types.impl.FirImplicitBuiltinTypeRef
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.calls.tasks.ExplicitReceiverKind
import org.jetbrains.kotlin.types.AbstractTypeChecker

internal class CandidateFactoriesAndCollectors(
    val candidateFactory: CandidateFactory,
    val resultCollector: CandidateCollector,
    val stubReceiverCandidateFactory: CandidateFactory?,
    val invokeReceiverCandidateFactory: CandidateFactory?,
    val invokeReceiverCollector: CandidateCollector?,
    val invokeBuiltinExtensionReceiverCandidateFactory: CandidateFactory?
)

typealias EnqueueTasksForInvokeReceiverCandidates = () -> Unit

internal class LevelHandler {

    // Try to avoid adding additional state here
    private var processResult = ProcessorAction.NONE

    fun handleLevel(
        info: CallInfo,
        explicitReceiverKind: ExplicitReceiverKind,
        group: TowerGroup,
        candidateFactoriesAndCollectors: CandidateFactoriesAndCollectors,
        towerLevel: SessionBasedTowerLevel,
        invokeResolveMode: InvokeResolveMode?,
        candidateFactory: CandidateFactory,
        enqueueResolverTasksForInvokeReceiverCandidates: EnqueueTasksForInvokeReceiverCandidates
    ): ProcessorAction {
        val resultCollector = candidateFactoriesAndCollectors.resultCollector
        val processor =
            TowerScopeLevelProcessor(
                info.explicitReceiver,
                explicitReceiverKind,
                resultCollector,
                candidateFactory,
                group
            )
        when (info.callKind) {
            CallKind.VariableAccess -> {
                towerLevel.processProperties(info.name, processor)

                if (!resultCollector.isSuccess()) {
                    towerLevel.processObjectsAsVariables(info.name, processor)
                }
            }
            CallKind.Function -> {
                val invokeBuiltinExtensionMode =
                    invokeResolveMode == InvokeResolveMode.RECEIVER_FOR_INVOKE_BUILTIN_EXTENSION

                if (!invokeBuiltinExtensionMode) {
                    towerLevel.processFunctions(info.name, processor)
                }

                if (invokeResolveMode == InvokeResolveMode.IMPLICIT_CALL_ON_GIVEN_RECEIVER ||
                    resultCollector.isSuccess()
                ) {
                    return processResult
                }

                val invokeReceiverProcessor = TowerScopeLevelProcessor(
                    info.explicitReceiver,
                    explicitReceiverKind,
                    candidateFactoriesAndCollectors.invokeReceiverCollector!!,
                    if (invokeBuiltinExtensionMode) candidateFactoriesAndCollectors.invokeBuiltinExtensionReceiverCandidateFactory!!
                    else candidateFactoriesAndCollectors.invokeReceiverCandidateFactory!!,
                    group
                )
                candidateFactoriesAndCollectors.invokeReceiverCollector.newDataSet()
                towerLevel.processProperties(info.name, invokeReceiverProcessor)
                towerLevel.processObjectsAsVariables(info.name, invokeReceiverProcessor)

                if (candidateFactoriesAndCollectors.invokeReceiverCollector.isSuccess()) {
                    enqueueResolverTasksForInvokeReceiverCandidates()
                }
            }
            CallKind.CallableReference -> {
                val stubReceiver = info.stubReceiver
                if (stubReceiver != null) {
                    val stubReceiverValue = ExpressionReceiverValue(stubReceiver)
                    val stubProcessor = TowerScopeLevelProcessor(
                        info.explicitReceiver,
                        if (towerLevel is MemberScopeTowerLevel && towerLevel.dispatchReceiver is AbstractExplicitReceiver<*>) {
                            ExplicitReceiverKind.DISPATCH_RECEIVER
                        } else {
                            ExplicitReceiverKind.EXTENSION_RECEIVER
                        },
                        resultCollector,
                        candidateFactoriesAndCollectors.stubReceiverCandidateFactory!!, group
                    )
                    val towerLevelWithStubReceiver = towerLevel.replaceReceiverValue(stubReceiverValue)
                    towerLevelWithStubReceiver.processFunctionsAndProperties(info.name, stubProcessor)
                    // NB: we don't perform this for implicit Unit
                    if (!resultCollector.isSuccess() && info.explicitReceiver?.typeRef !is FirImplicitBuiltinTypeRef) {
                        towerLevel.processFunctionsAndProperties(info.name, processor)
                    }
                } else {
                    towerLevel.processFunctionsAndProperties(info.name, processor)
                }
            }
            else -> {
                throw AssertionError("Unsupported call kind in tower resolver: ${info.callKind}")
            }
        }
        return processResult
    }

    private fun TowerScopeLevel.processProperties(
        name: Name,
        processor: TowerScopeLevel.TowerScopeLevelProcessor<AbstractFirBasedSymbol<*>>
    ) {
        processElementsByNameAndStoreResult(TowerScopeLevel.Token.Properties, name, processor)
    }

    private fun TowerScopeLevel.processFunctions(
        name: Name,
        processor: TowerScopeLevel.TowerScopeLevelProcessor<AbstractFirBasedSymbol<*>>
    ) {
        processElementsByNameAndStoreResult(TowerScopeLevel.Token.Functions, name, processor)
    }

    private fun TowerScopeLevel.processFunctionsAndProperties(
        name: Name, processor: TowerScopeLevel.TowerScopeLevelProcessor<AbstractFirBasedSymbol<*>>
    ) {
        processFunctions(name, processor)
        processProperties(name, processor)
    }

    private fun TowerScopeLevel.processObjectsAsVariables(
        name: Name, processor: TowerScopeLevel.TowerScopeLevelProcessor<AbstractFirBasedSymbol<*>>
    ) {
        // Skipping objects when extension receiver is bound to the level
        if (this is ScopeTowerLevel && this.extensionReceiver != null) return

        processElementsByNameAndStoreResult(TowerScopeLevel.Token.Objects, name, processor)
    }

    private fun <T : AbstractFirBasedSymbol<*>> TowerScopeLevel.processElementsByNameAndStoreResult(
        token: TowerScopeLevel.Token<T>,
        name: Name,
        processor: TowerScopeLevel.TowerScopeLevelProcessor<T>
    ): ProcessorAction {
        return processElementsByName(token, name, processor).also {
            processResult += it
        }
    }
}

private class TowerScopeLevelProcessor(
    val explicitReceiver: FirExpression?,
    val explicitReceiverKind: ExplicitReceiverKind,
    val resultCollector: CandidateCollector,
    val candidateFactory: CandidateFactory,
    val group: TowerGroup
) : TowerScopeLevel.TowerScopeLevelProcessor<AbstractFirBasedSymbol<*>> {
    override fun consumeCandidate(
        symbol: AbstractFirBasedSymbol<*>,
        dispatchReceiverValue: ReceiverValue?,
        implicitExtensionReceiverValue: ImplicitReceiverValue<*>?,
        builtInExtensionFunctionReceiverValue: ReceiverValue?
    ) {
        // Check explicit extension receiver for default package members
        if (symbol is FirNamedFunctionSymbol && dispatchReceiverValue == null &&
            (implicitExtensionReceiverValue == null) != (explicitReceiver == null) &&
            explicitReceiver !is FirResolvedQualifier &&
            symbol.callableId.packageName.startsWith(defaultPackage)
        ) {
            val extensionReceiverType = explicitReceiver?.typeRef?.coneTypeSafe()
                ?: implicitExtensionReceiverValue?.type as? ConeClassLikeType
            if (extensionReceiverType != null) {
                val declarationReceiverTypeRef =
                    (symbol as? FirCallableSymbol<*>)?.fir?.receiverTypeRef as? FirResolvedTypeRef
                val declarationReceiverType = declarationReceiverTypeRef?.type
                if (declarationReceiverType is ConeClassLikeType) {
                    if (!AbstractTypeChecker.isSubtypeOf(
                            candidateFactory.bodyResolveComponents.inferenceComponents.ctx,
                            extensionReceiverType,
                            declarationReceiverType.lookupTag.constructClassType(
                                declarationReceiverType.typeArguments.map { ConeStarProjection }.toTypedArray(),
                                isNullable = true
                            )
                        )
                    ) {
                        return
                    }
                }
            }
        }
        // ---
        resultCollector.consumeCandidate(
            group, candidateFactory.createCandidate(
                symbol,
                explicitReceiverKind,
                dispatchReceiverValue,
                implicitExtensionReceiverValue,
                builtInExtensionFunctionReceiverValue
            )
        )
    }

    companion object {
        val defaultPackage = Name.identifier("kotlin")
    }
}
