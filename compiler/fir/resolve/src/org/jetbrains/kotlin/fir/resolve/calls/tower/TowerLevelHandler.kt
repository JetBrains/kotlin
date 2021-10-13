/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.calls.tower

import org.jetbrains.kotlin.fir.resolve.calls.*
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol

internal class CandidateFactoriesAndCollectors(
    // Common calls
    val candidateFactory: CandidateFactory,
    val resultCollector: CandidateCollector,
)

internal fun TowerLevel.Processor<FirBasedSymbol<*>>.withSuccess(
    isSuccess: () -> Boolean
): TowerLevelHandler.Processor {
    return object : TowerLevelHandler.Processor {
        override val isSuccess: Boolean get() = isSuccess()

        override fun consumeSymbol(
            symbol: FirBasedSymbol<*>,
            levelContext: TowerLevel.Context
        ) {
            this@withSuccess.consumeSymbol(symbol, levelContext)
        }
    }
}

internal class TowerLevelHandler {

    // Try to avoid adding additional state here
    private var processResult = ProcessResult.SCOPE_EMPTY

    interface Processor : TowerLevel.Processor<FirBasedSymbol<*>> {
        val isSuccess: Boolean
    }

    fun handleLevel(
        info: CallInfo,
        towerLevel: SessionBasedTowerLevel,
        processor: Processor,
    ): ProcessResult {
        processResult = ProcessResult.SCOPE_EMPTY

        when (info.callKind) {
            CallKind.VariableAccess -> {
                processResult += towerLevel.processPropertiesByName(info, processor)

                if (!processor.isSuccess && towerLevel is ScopeTowerLevel && towerLevel.extensionReceiver == null) {
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
