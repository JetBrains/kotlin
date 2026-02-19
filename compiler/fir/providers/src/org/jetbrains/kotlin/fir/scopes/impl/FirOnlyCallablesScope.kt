/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.scopes.impl

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.scopes.DelicateScopeAPI
import org.jetbrains.kotlin.fir.scopes.FirContainingNamesAwareScope
import org.jetbrains.kotlin.fir.scopes.FirScope
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirVariableSymbol
import org.jetbrains.kotlin.name.Name

class FirOnlyCallablesScope(val delegate: FirScope) : FirScope() {
    override fun processFunctionsByName(name: Name, processor: (FirNamedFunctionSymbol) -> Unit) {
        return delegate.processFunctionsByName(name, processor)
    }

    override fun processPropertiesByName(name: Name, processor: (FirVariableSymbol<*>) -> Unit) {
        return delegate.processPropertiesByName(name, processor)
    }

    override val scopeOwnerLookupNames: List<String>
        get() = delegate.scopeOwnerLookupNames

    @DelicateScopeAPI
    override fun withReplacedSessionOrNull(newSession: FirSession, newScopeSession: ScopeSession): FirOnlyCallablesScope? {
        return delegate.withReplacedSessionOrNull(newSession, newScopeSession)?.let { FirOnlyCallablesScope(it) }
    }
}

class FirNameAwareOnlyCallablesScope(val delegate: FirContainingNamesAwareScope) : FirContainingNamesAwareScope() {
    // We want to *avoid* delegation to certain scope functions, so we delegate explicitly instead of using
    // `FirDelegatingContainingNamesAwareScope`.

    override fun processFunctionsByName(name: Name, processor: (FirNamedFunctionSymbol) -> Unit) {
        return delegate.processFunctionsByName(name, processor)
    }

    override fun processPropertiesByName(name: Name, processor: (FirVariableSymbol<*>) -> Unit) {
        return delegate.processPropertiesByName(name, processor)
    }

    override val scopeOwnerLookupNames: List<String>
        get() = delegate.scopeOwnerLookupNames

    override fun getCallableNames(): Set<Name> = delegate.getCallableNames()

    override fun getClassifierNames(): Set<Name> = emptySet()

    @DelicateScopeAPI
    override fun withReplacedSessionOrNull(newSession: FirSession, newScopeSession: ScopeSession): FirNameAwareOnlyCallablesScope? {
        return delegate.withReplacedSessionOrNull(newSession, newScopeSession)?.let { FirNameAwareOnlyCallablesScope(it) }
    }
}
