/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.scopes

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.resolve.substitution.ConeSubstitutor
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.name.Name

class FirFilteringNamesAwareScope(
    val original: FirContainingNamesAwareScope,
    val filter: (FirBasedSymbol<*>) -> Boolean
) : FirContainingNamesAwareScope() {
    override fun getCallableNames(): Set<Name> = original.getCallableNames()
    override fun getClassifierNames(): Set<Name> = original.getClassifierNames()

    override fun processClassifiersByNameWithSubstitution(name: Name, processor: (FirClassifierSymbol<*>, ConeSubstitutor) -> Unit) {
        original.processClassifiersByNameWithSubstitution(name) { symbol, substitutor ->
            if (filter(symbol)) processor(symbol, substitutor)
        }
    }

    override fun processDeclaredConstructors(processor: (FirConstructorSymbol) -> Unit) {
        original.processDeclaredConstructors {
            if (filter(it)) processor(it)
        }
    }

    override fun processFunctionsByName(name: Name, processor: (FirNamedFunctionSymbol) -> Unit) {
        original.processFunctionsByName(name) {
            if (filter(it)) processor(it)
        }
    }

    override fun processPropertiesByName(name: Name, processor: (FirVariableSymbol<*>) -> Unit) {
        original.processPropertiesByName(name) {
            if (filter(it)) processor(it)
        }
    }

    @DelicateScopeAPI
    override fun withReplacedSessionOrNull(newSession: FirSession, newScopeSession: ScopeSession): FirContainingNamesAwareScope? =
        original.withReplacedSessionOrNull(newSession, newScopeSession)?.let { FirFilteringNamesAwareScope(it, filter) }
}

class FirFilteringTypeScope(
    val original: FirTypeScope,
    val filter: (FirCallableSymbol<*>) -> Boolean
) : FirTypeScope() {
    override fun getCallableNames(): Set<Name> = original.getCallableNames()
    override fun getClassifierNames(): Set<Name> = original.getClassifierNames()

    override fun processDeclaredConstructors(processor: (FirConstructorSymbol) -> Unit) {
        original.processDeclaredConstructors {
            if (filter(it)) processor(it)
        }
    }

    override fun processFunctionsByName(name: Name, processor: (FirNamedFunctionSymbol) -> Unit) {
        original.processFunctionsByName(name) {
            if (filter(it)) processor(it)
        }
    }

    override fun processPropertiesByName(name: Name, processor: (FirVariableSymbol<*>) -> Unit) {
        original.processPropertiesByName(name) {
            if (filter(it)) processor(it)
        }
    }

    override fun processDirectOverriddenFunctionsWithBaseScope(
        functionSymbol: FirNamedFunctionSymbol,
        processor: (FirNamedFunctionSymbol, FirTypeScope) -> ProcessorAction,
    ): ProcessorAction = original.processDirectOverriddenFunctionsWithBaseScope(functionSymbol) { symbol, scope ->
        if (filter(symbol)) processor(symbol, scope) else ProcessorAction.NEXT
    }

    override fun processDirectOverriddenPropertiesWithBaseScope(
        propertySymbol: FirPropertySymbol,
        processor: (FirPropertySymbol, FirTypeScope) -> ProcessorAction,
    ): ProcessorAction = original.processDirectOverriddenPropertiesWithBaseScope(propertySymbol) { symbol, scope ->
        if (filter(symbol)) processor(symbol, scope) else ProcessorAction.NEXT
    }

    @DelicateScopeAPI
    override fun withReplacedSessionOrNull(newSession: FirSession, newScopeSession: ScopeSession, ): FirTypeScope? =
        original.withReplacedSessionOrNull(newSession, newScopeSession)?.let { FirFilteringTypeScope(it, filter) }
}