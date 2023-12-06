/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.scopes

import org.jetbrains.kotlin.fir.resolve.substitution.ConeSubstitutor
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.name.Name

/**
 * Special type scope for unstable smartcast. The purpose of this scope is only to report "SMARTCAST_IMPOSSIBLE" diagnostics.
 *
 * This scope will serve all candidates available in the original scope. In addition, it also serve all additional members that are
 * available from the smartcast type. This way, these additional members can be resolved. Later in
 * [org.jetbrains.kotlin.fir.resolve.calls.CheckDispatchReceiver], these additional members are rejected with "UnstableSmartcast"
 * diagnostic, which surfaces as "SMARTCAST_IMPOSSIBLE" diagnostic.
 */
class FirUnstableSmartcastTypeScope(
    private val smartcastScope: FirTypeScope,
    private val originalScope: FirTypeScope
) : FirTypeScope() {
    private val scopes = listOf(originalScope, smartcastScope)
    private val symbolsFromUnstableSmartcast = mutableSetOf<FirCallableSymbol<*>>()
    override fun processClassifiersByNameWithSubstitution(
        name: Name,
        processor: (FirClassifierSymbol<*>, ConeSubstitutor) -> Unit
    ) {
        for (scope in scopes) {
            scope.processClassifiersByNameWithSubstitution(name, processor)
        }
    }

    private inline fun <T : FirCallableSymbol<*>> processComposite(
        process: FirTypeScope.(Name, (T) -> Unit) -> Unit,
        name: Name,
        noinline processor: (T) -> Unit
    ) {
        val unique = mutableSetOf<T>()
        originalScope.process(name) {
            unique += it
            processor(it)
        }
        smartcastScope.process(name) {
            if (it !in unique) {
                markSymbolFromUnstableSmartcast(it)
                processor(it)
            }
        }
    }

    override fun processFunctionsByName(name: Name, processor: (FirNamedFunctionSymbol) -> Unit) {
        return processComposite(FirScope::processFunctionsByName, name, processor)
    }

    override fun processPropertiesByName(name: Name, processor: (FirVariableSymbol<*>) -> Unit) {
        return processComposite(FirScope::processPropertiesByName, name, processor)
    }

    private inline fun <S : FirCallableSymbol<*>> processDirectOverriddenWithBaseScope(
        originalSymbol: S,
        processDirectOverriddenSymbolsWithBaseScope: FirTypeScope.(S, (S, FirTypeScope) -> ProcessorAction) -> ProcessorAction,
        noinline processor: (S, FirTypeScope) -> ProcessorAction
    ): ProcessorAction {
        val unique = mutableSetOf<S>()
        originalScope.processDirectOverriddenSymbolsWithBaseScope(originalSymbol) { symbol, firTypeScope ->
            unique += symbol
            processor(symbol, firTypeScope)
        }.let { if (it == ProcessorAction.STOP) return ProcessorAction.STOP }

        smartcastScope.processDirectOverriddenSymbolsWithBaseScope(originalSymbol) { symbol, firTypeScope ->
            when (symbol) {
                /**
                 * `processDirectOverriddenSymbolsWithBaseScope` may return the same symbol from underlying scopes,
                 *   so we need to skip them until we found real overridden
                 */
                originalSymbol -> ProcessorAction.NEXT
                !in unique -> {
                    markSymbolFromUnstableSmartcast(symbol)
                    processor(symbol, firTypeScope)
                }
                else -> ProcessorAction.NEXT
            }
        }.let { if (it == ProcessorAction.STOP) return ProcessorAction.STOP }
        return ProcessorAction.NEXT
    }

    fun isSymbolFromUnstableSmartcast(symbol: FirBasedSymbol<*>) = symbol in symbolsFromUnstableSmartcast

    fun markSymbolFromUnstableSmartcast(symbol: FirCallableSymbol<*>) {
        symbolsFromUnstableSmartcast += symbol
    }

    override fun processDirectOverriddenFunctionsWithBaseScope(
        functionSymbol: FirNamedFunctionSymbol,
        processor: (FirNamedFunctionSymbol, FirTypeScope) -> ProcessorAction
    ): ProcessorAction {
        return processDirectOverriddenWithBaseScope(functionSymbol, FirTypeScope::processDirectOverriddenFunctionsWithBaseScope, processor)
    }

    override fun processDirectOverriddenPropertiesWithBaseScope(
        propertySymbol: FirPropertySymbol,
        processor: (FirPropertySymbol, FirTypeScope) -> ProcessorAction
    ): ProcessorAction {
        return processDirectOverriddenWithBaseScope(propertySymbol, FirTypeScope::processDirectOverriddenPropertiesWithBaseScope, processor)
    }

    override fun getCallableNames(): Set<Name> {
        return scopes.flatMapTo(hashSetOf()) { it.getCallableNames() }
    }

    override fun getClassifierNames(): Set<Name> {
        return scopes.flatMapTo(hashSetOf()) { it.getClassifierNames() }
    }

    override val scopeOwnerLookupNames: List<String> by lazy(LazyThreadSafetyMode.PUBLICATION) {
        scopes.flatMap { it.scopeOwnerLookupNames }
    }
}
