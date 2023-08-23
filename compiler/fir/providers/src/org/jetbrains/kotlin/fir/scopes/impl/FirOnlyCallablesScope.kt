/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.scopes.impl

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
}
