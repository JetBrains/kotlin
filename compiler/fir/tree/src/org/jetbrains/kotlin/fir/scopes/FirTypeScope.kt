/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.scopes

import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.name.Name

abstract class FirTypeScope : FirScope(), FirContainingNamesAwareScope {
    // Currently, this function and its property brother both have very weak guarantees
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

    // ------------------------------------------------------------------------------------

    abstract fun processOverriddenPropertiesWithDepth(
        propertySymbol: FirPropertySymbol,
        processor: (FirPropertySymbol, Int) -> ProcessorAction
    ): ProcessorAction

    inline fun processOverriddenProperties(
        propertySymbol: FirPropertySymbol,
        crossinline processor: (FirPropertySymbol) -> ProcessorAction
    ): ProcessorAction = processOverriddenPropertiesWithDepth(propertySymbol) { symbol, _ ->
        processor(symbol)
    }

    inline fun processDirectlyOverriddenProperties(
        propertySymbol: FirPropertySymbol,
        crossinline processor: (FirPropertySymbol) -> ProcessorAction
    ): ProcessorAction = processOverriddenPropertiesWithDepth(propertySymbol) { symbol, depth ->
        if (depth == 1) {
            processor(symbol)
        } else {
            ProcessorAction.NEXT
        }
    }

    // ------------------------------------------------------------------------------------

    // This is just a helper for a common implementation
    protected fun <S : FirCallableSymbol<*>> doProcessOverriddenCallables(
        callableSymbol: S,
        processor: (S, Int) -> ProcessorAction,
        directOverriddenMap: Map<S, Collection<S>>,
        baseScope: FirTypeScope,
        processOverriddenCallablesWithDepth: FirTypeScope.(S, (S, Int) -> ProcessorAction) -> ProcessorAction
    ): ProcessorAction {
        for (overridden in directOverriddenMap[callableSymbol].orEmpty()) {
            val overriddenDepth = if (overridden.overriddenSymbol != null) 0 else 1
            if (!processor(overridden, overriddenDepth)) return ProcessorAction.STOP
            if (!baseScope.processOverriddenCallablesWithDepth(overridden) { symbol, depth ->
                    processor(symbol, depth + overriddenDepth)
                }
            ) return ProcessorAction.STOP
        }

        return baseScope.processOverriddenCallablesWithDepth(callableSymbol, processor)
    }

    object Empty : FirTypeScope() {
        override fun processOverriddenFunctionsWithDepth(
            functionSymbol: FirFunctionSymbol<*>,
            processor: (FirFunctionSymbol<*>, Int) -> ProcessorAction
        ): ProcessorAction = ProcessorAction.NEXT

        override fun processOverriddenPropertiesWithDepth(
            propertySymbol: FirPropertySymbol,
            processor: (FirPropertySymbol, Int) -> ProcessorAction
        ): ProcessorAction = ProcessorAction.NEXT

        override fun getCallableNames(): Set<Name> = emptySet()

        override fun getClassifierNames(): Set<Name> = emptySet()
    }
}
