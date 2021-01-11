/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.calls.tower

import org.jetbrains.kotlin.fir.resolve.calls.*
import org.jetbrains.kotlin.fir.scopes.FirScope
import org.jetbrains.kotlin.fir.scopes.ProcessorAction
import org.jetbrains.kotlin.fir.symbols.AbstractFirBasedSymbol
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.calls.tasks.ExplicitReceiverKind

internal class CandidateFactoriesAndCollectors(
    // Common calls
    val candidateFactory: CandidateFactory,
    val resultCollector: CandidateCollector,
)


internal class TowerLevelHandler {

    // Try to avoid adding additional state here
    private var processResult = ProcessorAction.NONE

    fun handleLevel(
        collector: CandidateCollector,
        candidateFactory: CandidateFactory,
        info: CallInfo,
        explicitReceiverKind: ExplicitReceiverKind,
        group: TowerGroup,
        towerLevel: SessionBasedTowerLevel
    ): ProcessorAction {
        processResult = ProcessorAction.NONE
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
                processResult += towerLevel.processPropertiesByName(info.name, processor)

                if (!collector.isSuccess() && towerLevel is ScopeTowerLevel && towerLevel.extensionReceiver == null) {
                    processResult += towerLevel.processObjectsByName(info.name, processor)
                }
            }
            CallKind.Function -> {
                processResult += towerLevel.processFunctionsByName(info.name, processor)
            }
            CallKind.CallableReference -> {
                processResult += towerLevel.processFunctionsByName(info.name, processor)
                processResult += towerLevel.processPropertiesByName(info.name, processor)
            }
            else -> {
                throw AssertionError("Unsupported call kind in tower resolver: ${info.callKind}")
            }
        }
        return processResult
    }
}

private class TowerScopeLevelProcessor(
    override val callInfo: CallInfo,
    val explicitReceiverKind: ExplicitReceiverKind,
    val resultCollector: CandidateCollector,
    val candidateFactory: CandidateFactory,
    val group: TowerGroup
) : TowerScopeLevel.TowerScopeLevelProcessor<AbstractFirBasedSymbol<*>> {
    override fun consumeCandidate(
        symbol: AbstractFirBasedSymbol<*>,
        dispatchReceiverValue: ReceiverValue?,
        extensionReceiverValue: ReceiverValue?,
        scope: FirScope,
        builtInExtensionFunctionReceiverValue: ReceiverValue?
    ) {
        resultCollector.consumeCandidate(
            group, candidateFactory.createCandidate(
                callInfo,
                symbol,
                explicitReceiverKind,
                scope,
                dispatchReceiverValue,
                extensionReceiverValue,
                builtInExtensionFunctionReceiverValue
            ), candidateFactory.context
        )
    }

    companion object {
        val defaultPackage = Name.identifier("kotlin")
    }
}
