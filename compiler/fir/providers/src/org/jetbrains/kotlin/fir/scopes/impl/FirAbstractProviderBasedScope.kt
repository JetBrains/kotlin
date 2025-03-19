/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.scopes.impl

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.resolve.providers.FirSymbolProvider
import org.jetbrains.kotlin.fir.resolve.providers.dependenciesSymbolProvider
import org.jetbrains.kotlin.fir.resolve.providers.symbolProvider
import org.jetbrains.kotlin.fir.scopes.DelicateScopeAPI
import org.jetbrains.kotlin.fir.scopes.FirScope

abstract class FirAbstractProviderBasedScope(val session: FirSession, lookupInFir: Boolean = true) : FirScope() {
    // TODO (marco): Using the `dependenciesSymbolProvider` is just an optimization, according to
    //  `FirLookupDefaultStarImportsInSourcesSettingHolder`, anyway. Unfortunately, it's NOT an optimization for the Analysis API with
    //  unified symbol providers, since the `dependenciesSymbolProvider` is a relatively expensive view on the unified symbol provider.
    //  We obviously cannot just disable this optimization for the compiler, so we'll have to specifically disable it in the Analysis API.
//    val provider: FirSymbolProvider = when {
//        lookupInFir -> session.symbolProvider
//        else -> session.dependenciesSymbolProvider
//    }
    val provider: FirSymbolProvider = session.symbolProvider

    @DelicateScopeAPI
    abstract override fun withReplacedSessionOrNull(newSession: FirSession, newScopeSession: ScopeSession): FirAbstractProviderBasedScope?
}
