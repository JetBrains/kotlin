/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.scopes.impl

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.DirectDeclarationsAccess
import org.jetbrains.kotlin.fir.declarations.FirClass
import org.jetbrains.kotlin.fir.declarations.FirConstructor
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.scopes.DelicateScopeAPI
import org.jetbrains.kotlin.fir.scopes.FirDelegatingTypeScope
import org.jetbrains.kotlin.fir.scopes.FirTypeScope
import org.jetbrains.kotlin.fir.symbols.impl.FirConstructorSymbol

@OptIn(DirectDeclarationsAccess::class)
class FirTrivialEnumEntryScope(
    private val klass: FirClass,
    private val delegate: FirTypeScope,
) : FirDelegatingTypeScope(delegate) {
    override fun processDeclaredConstructors(processor: (FirConstructorSymbol) -> Unit) {
        for (declaration in klass.declarations) {
            if (declaration is FirConstructor) {
                processor(declaration.symbol)
            }
        }
    }

    @DelicateScopeAPI
    override fun withReplacedSessionOrNull(
        newSession: FirSession,
        newScopeSession: ScopeSession,
    ): FirDelegatingTypeScope? {
        return delegate.withReplacedSessionOrNull(newSession, newScopeSession)?.let {
            FirTrivialEnumEntryScope(klass, it)
        }
    }
}