/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.scopes

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.resolve.substitution.ConeSubstitutor
import org.jetbrains.kotlin.fir.symbols.impl.FirClassifierSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirConstructorSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirVariableSymbol
import org.jetbrains.kotlin.name.Name

class FirCompositeScope(val scopes: Iterable<FirScope>) : FirScope() {
    override fun processClassifiersByNameWithSubstitution(
        name: Name,
        processor: (FirClassifierSymbol<*>, ConeSubstitutor) -> Unit
    ) {
        for (scope in scopes) {
            scope.processClassifiersByNameWithSubstitution(name, processor)
        }
    }

    private inline fun <T> processComposite(
        process: FirScope.(Name, (T) -> Unit) -> Unit,
        name: Name,
        noinline processor: (T) -> Unit
    ) {
        val unique = mutableSetOf<T>()
        for (scope in scopes) {
            scope.process(name) {
                if (unique.add(it)) {
                    processor(it)
                }
            }
        }
    }

    private inline fun <T> processComposite(
        process: FirScope.((T) -> Unit) -> Unit,
        noinline processor: (T) -> Unit
    ) {
        val unique = mutableSetOf<T>()
        for (scope in scopes) {
            scope.process {
                if (unique.add(it)) {
                    processor(it)
                }
            }
        }
    }

    override fun processFunctionsByName(name: Name, processor: (FirNamedFunctionSymbol) -> Unit) {
        return processComposite(FirScope::processFunctionsByName, name, processor)
    }

    override fun processPropertiesByName(name: Name, processor: (FirVariableSymbol<*>) -> Unit) {
        return processComposite(FirScope::processPropertiesByName, name, processor)
    }

    override fun processDeclaredConstructors(processor: (FirConstructorSymbol) -> Unit) {
        processComposite(FirScope::processDeclaredConstructors, processor)
    }

    override val scopeOwnerLookupNames: List<String> by lazy(LazyThreadSafetyMode.PUBLICATION) {
        scopes.flatMap { it.scopeOwnerLookupNames }
    }

    @DelicateScopeAPI
    override fun withReplacedSessionOrNull(newSession: FirSession, newScopeSession: ScopeSession): FirCompositeScope? {
        val newScopes = scopes.withReplacedSessionOrNull(newSession, newScopeSession) ?: return null
        return FirCompositeScope(newScopes)
    }
}

class FirNameAwareCompositeScope(val scopes: Iterable<FirContainingNamesAwareScope>) : FirContainingNamesAwareScope() {
    private val delegate = FirCompositeScope(scopes)

    override fun processClassifiersByNameWithSubstitution(
        name: Name,
        processor: (FirClassifierSymbol<*>, ConeSubstitutor) -> Unit
    ) {
        delegate.processClassifiersByNameWithSubstitution(name, processor)
    }

    override fun processFunctionsByName(name: Name, processor: (FirNamedFunctionSymbol) -> Unit) {
        delegate.processFunctionsByName(name, processor)
    }

    override fun processPropertiesByName(name: Name, processor: (FirVariableSymbol<*>) -> Unit) {
        delegate.processPropertiesByName(name, processor)
    }

    override fun processDeclaredConstructors(processor: (FirConstructorSymbol) -> Unit) {
        delegate.processDeclaredConstructors(processor)
    }

    override val scopeOwnerLookupNames: List<String>
        get() = delegate.scopeOwnerLookupNames

    override fun getCallableNames(): Set<Name> {
        return scopes.flatMapTo(hashSetOf()) { it.getCallableNames() }
    }

    override fun getClassifierNames(): Set<Name> {
        return scopes.flatMapTo(hashSetOf()) { it.getClassifierNames() }
    }

    @DelicateScopeAPI
    override fun withReplacedSessionOrNull(newSession: FirSession, newScopeSession: ScopeSession): FirNameAwareCompositeScope? {
        val newScopes = scopes.withReplacedSessionOrNull(newSession, newScopeSession) ?: return null
        return FirNameAwareCompositeScope(newScopes)
    }
}

@DelicateScopeAPI
inline fun <reified S : FirScope> Iterable<S>.withReplacedSessionOrNull(
    newSession: FirSession,
    newScopeSession: ScopeSession
): List<S>? {
    var wasUpdated = false
    val newScopes = this.map {
        it.withReplacedSessionOrNull(newSession, newScopeSession)?.also { newScope ->
            wasUpdated = true
        } as S? ?: it
    }
    return if (wasUpdated) newScopes else null
}
