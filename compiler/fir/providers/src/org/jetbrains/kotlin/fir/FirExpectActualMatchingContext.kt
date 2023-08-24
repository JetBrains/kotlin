/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir

import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirConstructorSymbol
import org.jetbrains.kotlin.mpp.RegularClassSymbolMarker
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.calls.mpp.ExpectActualMatchingContext

interface FirExpectActualMatchingContext : ExpectActualMatchingContext<FirBasedSymbol<*>> {
    fun FirClassSymbol<*>.getConstructors(
        scopeSession: ScopeSession,
        session: FirSession = moduleData.session,
    ): Collection<FirConstructorSymbol>

    override fun RegularClassSymbolMarker.getMembersForExpectClass(name: Name): List<FirCallableSymbol<*>>
}

interface FirExpectActualMatchingContextFactory : FirSessionComponent {
    fun create(
        session: FirSession,
        scopeSession: ScopeSession,
        allowedWritingMemberExpectForActualMapping: Boolean = false,
    ): FirExpectActualMatchingContext
}

val FirSession.expectActualMatchingContextFactory: FirExpectActualMatchingContextFactory by FirSession.sessionComponentAccessor()