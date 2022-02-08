/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.calls.tower

import org.jetbrains.kotlin.fir.resolve.calls.*
import org.jetbrains.kotlin.fir.scopes.FirScope
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.calls.tasks.ExplicitReceiverKind

internal class CandidateFactoriesAndCollectors(
    // Common calls
    val candidateFactory: CandidateFactory,
    val resultCollector: CandidateCollector,
)


internal class TowerLevelHandler {

    // Try to avoid adding additional state here
    private var processResult = ProcessResult.SCOPE_EMPTY

    fun handleLevel(
        collector: CandidateCollector,
        candidateFactory: CandidateFactory,
        info: CallInfo,
        explicitReceiverKind: ExplicitReceiverKind,
        group: TowerGroup,
        towerLevel: SessionBasedTowerLevel
    ): ProcessResult {
        processResult = ProcessResult.SCOPE_EMPTY
        val processor =
            TowerScopeLevelProcessor(
                info,
                explicitReceiverKind,
                collector,
                candidateFactory,
                group
            )

        when (info.callKind) {
            CallKind.VariableAccess -> {
                processResult += towerLevel.processPropertiesByName(info, processor)

                if (!collector.isSuccess() && towerLevel is ScopeTowerLevel && towerLevel.extensionReceiver == null) {
                    processResult += towerLevel.processObjectsByName(info, processor)
                }
            }
            CallKind.Function -> {
                processResult += towerLevel.processFunctionsByName(info, processor)
            }
            CallKind.CallableReference -> {
                processResult += towerLevel.processFunctionsByName(info, processor)
                processResult += towerLevel.processPropertiesByName(info, processor)
            }
            else -> {
                throw AssertionError("Unsupported call kind in tower resolver: ${info.callKind}")
            }
        }
        return processResult
    }
}

private class TowerScopeLevelProcessor(
    val callInfo: CallInfo,
    val explicitReceiverKind: ExplicitReceiverKind,
    val resultCollector: CandidateCollector,
    val candidateFactory: CandidateFactory,
    val group: TowerGroup
) : TowerScopeLevel.TowerScopeLevelProcessor<FirBasedSymbol<*>> {
    override fun consumeCandidate(
        symbol: FirBasedSymbol<*>,
        dispatchReceiverValue: ReceiverValue?,
        extensionReceiverValue: ReceiverValue?,
        scope: FirScope,
        builtInExtensionFunctionReceiverValue: ReceiverValue?,
        objectsByName: Boolean
    ) {
        resultCollector.consumeCandidate(
            group, candidateFactory.createCandidate(
                callInfo,
                symbol,
                explicitReceiverKind,
                scope,
                dispatchReceiverValue,
                extensionReceiverValue,
                builtInExtensionFunctionReceiverValue,
                objectsByName
            ), candidateFactory.context
        )
    }

    companion object {
        val defaultPackage = Name.identifier("kotlin")
    }
}
