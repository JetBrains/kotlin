/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.scopes.impl

import org.jetbrains.kotlin.fir.declarations.FirCallableDeclaration
import org.jetbrains.kotlin.fir.isCopyCreatedInScope
import org.jetbrains.kotlin.fir.scopes.CallableCopyTypeCalculator
import org.jetbrains.kotlin.fir.scopes.FirDelegatingTypeScope
import org.jetbrains.kotlin.fir.scopes.FirTypeScope
import org.jetbrains.kotlin.fir.scopes.ProcessorAction
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.name.Name

/**
 * This scope is a wrapper which is intended to use with scopes that can create callable copies.
 *
 * The main purpose of this scope is to update dispatched callables return types
 * in case it is not yet calculated due to implicit body resolve logic.
 */
class FirScopeWithCallableCopyReturnTypeUpdater(
    private val delegate: FirTypeScope,
    private val callableCopyTypeCalculator: CallableCopyTypeCalculator
) : FirDelegatingTypeScope(delegate) {
    override fun processFunctionsByName(name: Name, processor: (FirNamedFunctionSymbol) -> Unit) {
        delegate.processFunctionsByName(name) {
            updateReturnType(it.fir)
            processor(it)
        }
    }

    override fun processPropertiesByName(name: Name, processor: (FirVariableSymbol<*>) -> Unit) {
        delegate.processPropertiesByName(name) {
            updateReturnType(it.fir)
            processor(it)
        }
    }

    override fun processDirectOverriddenFunctionsWithBaseScope(
        functionSymbol: FirNamedFunctionSymbol,
        processor: (FirNamedFunctionSymbol, FirTypeScope) -> ProcessorAction
    ): ProcessorAction {
        return delegate.processDirectOverriddenFunctionsWithBaseScope(functionSymbol) { symbol, scope ->
            updateReturnType(symbol.fir)
            processor(symbol, scope)
        }
    }

    override fun processDirectOverriddenPropertiesWithBaseScope(
        propertySymbol: FirPropertySymbol,
        processor: (FirPropertySymbol, FirTypeScope) -> ProcessorAction
    ): ProcessorAction {
        return delegate.processDirectOverriddenPropertiesWithBaseScope(propertySymbol) { symbol, scope ->
            updateReturnType(symbol.fir)
            processor(symbol, scope)
        }
    }

    private fun updateReturnType(declaration: FirCallableDeclaration) {
        if (declaration.isCopyCreatedInScope) {
            callableCopyTypeCalculator.computeReturnType(declaration)
        }
    }

    override fun toString(): String {
        return delegate.toString()
    }
}
