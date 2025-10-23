/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.scopes.impl

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.getSingleMatchedExpectForActualOrNull
import org.jetbrains.kotlin.fir.declarations.utils.isActual
import org.jetbrains.kotlin.fir.declarations.utils.isExpect
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.resolve.calls.overloads.ConeEquivalentCallConflictResolver
import org.jetbrains.kotlin.fir.resolve.substitution.ConeSubstitutor
import org.jetbrains.kotlin.fir.scopes.DelicateScopeAPI
import org.jetbrains.kotlin.fir.scopes.FirScope
import org.jetbrains.kotlin.fir.scopes.FirTypeScope
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.name.Name

/**
 * A scope that filters out `expect` callables that have matching `actual` callables.
 *
 * In principle, it's not necessary to do this during resolution.
 * If we have multiple successful candidates, and they are matching expect and actual declarations, the expect ones will be filtered out by
 * [ConeEquivalentCallConflictResolver].
 *
 * However, there are special situations when the actual declaration is annotated with `@Deprecated(HIDDEN)`,
 * `@LowPriorityInOverloadResolution` or other annotations that influence resolution, and the expect declaration is not.
 * In these situations, the expect declarations could be among the successful candidates while the actual one could not.
 *
 * That's why it's necessary to eliminate the redundant expect declarations during resolution, not during overload conflict resolution.
 */
class FirActualizingScope(
    private val delegate: FirScope,
    private val session: FirSession,
) : FirScope() {
    init {
        // Cases with `FirTypeScope` should be handled by `MemberScopeTowerLevel`
        require(delegate !is FirTypeScope)
    }

    override fun mayContainName(name: Name): Boolean = delegate.mayContainName(name)
    override val scopeOwnerLookupNames: List<String> = delegate.scopeOwnerLookupNames

    override fun processClassifiersByNameWithSubstitution(name: Name, processor: (FirClassifierSymbol<*>, ConeSubstitutor) -> Unit) {
        // No filtering is needed for classifiers because the first classifier always wins, and `actual` classifier is always first in platform source-set
        delegate.processClassifiersByNameWithSubstitution(name, processor)
    }

    override fun processDeclaredConstructors(processor: (FirConstructorSymbol) -> Unit) {
        delegate.processDeclaredConstructors(processor)
    }

    override fun processFunctionsByName(name: Name, processor: (FirNamedFunctionSymbol) -> Unit) {
        processCallableSymbolsByName(name, FirScope::processFunctionsByName, processor)
    }

    override fun processPropertiesByName(name: Name, processor: (FirVariableSymbol<*>) -> Unit) {
        processCallableSymbolsByName(name, FirScope::processPropertiesByName, processor)
    }

    private fun <S : FirCallableSymbol<*>> processCallableSymbolsByName(
        name: Name,
        processingFactory: FirScope.(Name, (S) -> Unit) -> Unit,
        processor: (S) -> Unit,
    ) {
        val expectSymbols = mutableSetOf<S>()
        val notExpectSymbols = mutableSetOf<S>()
        // All matched `expect` callables should be preserved to make it possible to filter them out later if corresponding actuals are found
        val ignoredExpectSymbols = mutableSetOf<FirBasedSymbol<*>>()

        delegate.processingFactory(name) { symbol ->
            if (symbol.isActual) {
                val matchedExpectSymbol = symbol.getSingleMatchedExpectForActualOrNull()
                if (matchedExpectSymbol != null) {
                    expectSymbols.remove(matchedExpectSymbol) // Filter out matched expects candidates
                    ignoredExpectSymbols.add(matchedExpectSymbol)
                }
            } else if (symbol.isExpect && symbol in ignoredExpectSymbols) {
                // Skip the found `expect` because there is already a matched actual
                return@processingFactory
            }

            val resultSet = if (symbol.isExpect) expectSymbols else notExpectSymbols
            resultSet.add(symbol)
        }

        // When the symbols come from binary dependencies, the `isActual` flag is not preserved
        // (and getSingleMatchedExpectForActualOrNull wouldn't work anyway).
        // Filter out expect declarations from binary dependencies by manually matching them against non-expect ones
        // from binary dependencies.
        // It's important not to touch expect declarations from sources because they could match declarations from dependencies on accident.
        // See compiler/fir/analysis-tests/testData/resolve/multiplatform/redeclarationOfExpectActualFromDependency.kt
        expectSymbols.removeIf { expectSymbol ->
            expectSymbol.isFromLibrary() && expectSymbol.hasEquivalentNotActualFromLibrary(notExpectSymbols)
        }

        expectSymbols.forEach(processor)
        notExpectSymbols.forEach(processor)
    }

    private fun FirCallableSymbol<*>.hasEquivalentNotActualFromLibrary(notExpectSymbols: Set<FirCallableSymbol<*>>): Boolean {
        return notExpectSymbols.any { it.isFromLibrary() && areEquivalent(this, it) }
    }

    private fun FirCallableSymbol<*>.isFromLibrary(): Boolean {
        return moduleData.session.kind == FirSession.Kind.Library
    }

    private fun <S : FirCallableSymbol<*>> areEquivalent(symbol: S, s: S): Boolean {
        return ConeEquivalentCallConflictResolver.areEquivalentTopLevelCallables(
            symbol.fir,
            s.fir,
            session,
            argumentMappingIsEqual = null
        )
    }

    @DelicateScopeAPI
    override fun withReplacedSessionOrNull(newSession: FirSession, newScopeSession: ScopeSession): FirActualizingScope? {
        return delegate.withReplacedSessionOrNull(newSession, newScopeSession)?.let { FirActualizingScope(it, newSession) }
    }
}