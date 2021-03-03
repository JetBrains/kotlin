/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.frontend.api.components

import org.jetbrains.kotlin.idea.frontend.api.symbols.KtCallableSymbol
import org.jetbrains.kotlin.idea.frontend.api.symbols.KtClassOrObjectSymbol
import org.jetbrains.kotlin.idea.frontend.api.symbols.KtSymbol

abstract class KtSymbolDeclarationOverridesProvider : KtAnalysisSessionComponent() {
    abstract fun <T : KtSymbol> getAllOverriddenSymbols(callableSymbol: T): List<KtCallableSymbol>
    abstract fun <T : KtSymbol> getDirectlyOverriddenSymbols(callableSymbol: T): List<KtCallableSymbol>

    abstract fun getIntersectionOverriddenSymbols(symbol: KtCallableSymbol): Collection<KtCallableSymbol>
}

interface KtSymbolDeclarationOverridesProviderMixIn : KtAnalysisSessionMixIn {
    /**
     * Return a list of **all** symbols which are overridden by symbol
     *
     * E.g, if we have `A.foo` overrides `B.foo` overrides `C.foo`, all two super declarations `B.foo`, `C.foo` will be returned
     *
     * Unwraps substituted overridden symbols (see [org.jetbrains.kotlin.idea.frontend.api.symbols.KtSymbolOrigin.INTERSECTION_OVERRIDE])
     *
     * @see getDirectlyOverriddenSymbols
     */
    fun KtCallableSymbol.getAllOverriddenSymbols(): List<KtCallableSymbol> =
        analysisSession.symbolDeclarationOverridesProvider.getAllOverriddenSymbols(this)

    /**
     * Return a list of symbols which are **directly** overridden by symbol
     **
     * E.g, if we have `A.foo` overrides `B.foo` overrides `C.foo`, only declarations directly overridden `B.foo` will be returned
     *
     * Unwraps substituted overridden symbols (see [org.jetbrains.kotlin.idea.frontend.api.symbols.KtSymbolOrigin.INTERSECTION_OVERRIDE])
     *
     *  @see getAllOverriddenSymbols
     */
    fun KtCallableSymbol.getDirectlyOverriddenSymbols(): List<KtCallableSymbol> =
        analysisSession.symbolDeclarationOverridesProvider.getDirectlyOverriddenSymbols(this)

    fun KtCallableSymbol.getIntersectionOverriddenSymbols(): Collection<KtCallableSymbol> =
        analysisSession.symbolDeclarationOverridesProvider.getIntersectionOverriddenSymbols(this)
}