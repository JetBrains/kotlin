/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.scopes

import org.jetbrains.kotlin.fir.symbols.impl.FirFunctionSymbol

abstract class FirTypeScope : FirScope() {
    // Currently, this function has very weak guarantees
    // - It may silently do nothing on symbols originated from different scope instance
    // - It may return the same overridden symbols more then once in case of substitution
    // - It doesn't guarantee any specific order in which overridden tree will be traversed
    // But if the scope instance is the same as the one from which the symbol was originated, this function will enumarate all members
    // of the overridden tree
    abstract fun processOverriddenFunctions(
        functionSymbol: FirFunctionSymbol<*>,
        processor: (FirFunctionSymbol<*>) -> ProcessorAction
    ): ProcessorAction

    // This is just a helper for a common implementation
    protected fun doProcessOverriddenFunctions(
        functionSymbol: FirFunctionSymbol<*>,
        processor: (FirFunctionSymbol<*>) -> ProcessorAction,
        directOverriddenMap: Map<FirFunctionSymbol<*>, Collection<FirFunctionSymbol<*>>>,
        baseScope: FirTypeScope
    ): ProcessorAction {
        val directOverridden =
            directOverriddenMap[functionSymbol] ?: return baseScope.processOverriddenFunctions(functionSymbol, processor)

        for (overridden in directOverridden) {
            if (!processor(overridden)) return ProcessorAction.STOP
            if (!baseScope.processOverriddenFunctions(overridden, processor)) return ProcessorAction.STOP
        }

        return baseScope.processOverriddenFunctions(functionSymbol, processor)
    }
}
