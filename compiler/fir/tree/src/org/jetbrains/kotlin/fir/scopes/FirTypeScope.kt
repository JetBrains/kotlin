/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.scopes

import org.jetbrains.kotlin.fir.symbols.impl.FirFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol

abstract class FirTypeScope : FirScope() {
    // Currently, this function has very weak guarantees
    // - It may silently do nothing on symbols originated from different scope instance
    // - It may return the same overridden symbols more then once in case of substitution
    // - It doesn't guarantee any specific order in which overridden tree will be traversed
    // But if the scope instance is the same as the one from which the symbol was originated, this function will enumarate all members
    // of the overridden tree
    abstract fun processOverriddenFunctionsWithDepth(
        functionSymbol: FirFunctionSymbol<*>,
        processor: (FirFunctionSymbol<*>, Int) -> ProcessorAction
    ): ProcessorAction

    inline fun processOverriddenFunctions(
        functionSymbol: FirFunctionSymbol<*>,
        crossinline processor: (FirFunctionSymbol<*>) -> ProcessorAction
    ): ProcessorAction = processOverriddenFunctionsWithDepth(functionSymbol) { symbol, _ ->
        processor(symbol)
    }

    inline fun processDirectlyOverriddenFunctions(
        functionSymbol: FirFunctionSymbol<*>,
        crossinline processor: (FirFunctionSymbol<*>) -> ProcessorAction
    ): ProcessorAction = processOverriddenFunctionsWithDepth(functionSymbol) { symbol, depth ->
        if (depth == 1) {
            processor(symbol)
        } else {
            ProcessorAction.NEXT
        }
    }

    // This is just a helper for a common implementation
    protected fun doProcessOverriddenFunctions(
        functionSymbol: FirFunctionSymbol<*>,
        processor: (FirFunctionSymbol<*>, Int) -> ProcessorAction,
        directOverriddenMap: Map<FirFunctionSymbol<*>, Collection<FirFunctionSymbol<*>>>,
        baseScope: FirTypeScope
    ): ProcessorAction {
        val directOverridden =
            directOverriddenMap[functionSymbol] ?: return baseScope.processOverriddenFunctionsWithDepth(functionSymbol, processor)

        for (overridden in directOverridden) {
            val overriddenDepth = if (overridden is FirNamedFunctionSymbol && overridden.isFakeOverride) 0 else 1
            if (!processor(overridden, overriddenDepth)) return ProcessorAction.STOP
            if (!baseScope.processOverriddenFunctionsWithDepth(overridden) { symbol, depth ->
                    processor(symbol, depth + overriddenDepth)
                }
            ) return ProcessorAction.STOP
        }

        return baseScope.processOverriddenFunctionsWithDepth(functionSymbol, processor)
    }

    object Empty : FirTypeScope() {
        override fun processOverriddenFunctionsWithDepth(
            functionSymbol: FirFunctionSymbol<*>,
            processor: (FirFunctionSymbol<*>, Int) -> ProcessorAction
        ): ProcessorAction = ProcessorAction.NEXT
    }
}
