/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.components

import org.jetbrains.kotlin.analysis.api.symbols.KtCallableSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtClassOrObjectSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtSymbol

public abstract class KtSymbolDeclarationOverridesProvider : KtAnalysisSessionComponent() {
    public abstract fun <T : KtSymbol> getAllOverriddenSymbols(callableSymbol: T): List<KtCallableSymbol>
    public abstract fun <T : KtSymbol> getDirectlyOverriddenSymbols(callableSymbol: T): List<KtCallableSymbol>

    public abstract fun isSubClassOf(subClass: KtClassOrObjectSymbol, superClass: KtClassOrObjectSymbol): Boolean
    public abstract fun isDirectSubClassOf(subClass: KtClassOrObjectSymbol, superClass: KtClassOrObjectSymbol): Boolean

    public abstract fun getIntersectionOverriddenSymbols(symbol: KtCallableSymbol): Collection<KtCallableSymbol>
}

public interface KtSymbolDeclarationOverridesProviderMixIn : KtAnalysisSessionMixIn {
    /**
     * Return a list of **all** symbols which are overridden by symbol
     *
     * E.g, if we have `A.foo` overrides `B.foo` overrides `C.foo`, all two super declarations `B.foo`, `C.foo` will be returned
     *
     * Unwraps substituted overridden symbols (see [org.jetbrains.kotlin.analysis.api.symbols.KtSymbolOrigin.INTERSECTION_OVERRIDE])
     *
     * @see getDirectlyOverriddenSymbols
     */
    public fun KtCallableSymbol.getAllOverriddenSymbols(): List<KtCallableSymbol> =
        analysisSession.symbolDeclarationOverridesProvider.getAllOverriddenSymbols(this)

    /**
     * Return a list of symbols which are **directly** overridden by symbol
     **
     * E.g, if we have `A.foo` overrides `B.foo` overrides `C.foo`, only declarations directly overridden `B.foo` will be returned
     *
     * Unwraps substituted overridden symbols (see [org.jetbrains.kotlin.analysis.api.symbols.KtSymbolOrigin.INTERSECTION_OVERRIDE])
     *
     *  @see getAllOverriddenSymbols
     */
    public fun KtCallableSymbol.getDirectlyOverriddenSymbols(): List<KtCallableSymbol> =
        analysisSession.symbolDeclarationOverridesProvider.getDirectlyOverriddenSymbols(this)

    public fun KtClassOrObjectSymbol.isSubClassOf(superClass: KtClassOrObjectSymbol): Boolean =
        analysisSession.symbolDeclarationOverridesProvider.isSubClassOf(this, superClass)

    public fun KtClassOrObjectSymbol.isDirectSubClassOf(superClass: KtClassOrObjectSymbol): Boolean =
        analysisSession.symbolDeclarationOverridesProvider.isDirectSubClassOf(this, superClass)

    public fun KtCallableSymbol.getIntersectionOverriddenSymbols(): Collection<KtCallableSymbol> =
        analysisSession.symbolDeclarationOverridesProvider.getIntersectionOverriddenSymbols(this)
}