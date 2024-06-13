/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.components

import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.symbols.KaCallableSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaClassOrObjectSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaSymbol

public abstract class KaSymbolDeclarationOverridesProvider : KaSessionComponent() {
    public abstract fun <T : KaSymbol> getAllOverriddenSymbols(callableSymbol: T): List<KaCallableSymbol>
    public abstract fun <T : KaSymbol> getDirectlyOverriddenSymbols(callableSymbol: T): List<KaCallableSymbol>

    public abstract fun isSubClassOf(subClass: KaClassOrObjectSymbol, superClass: KaClassOrObjectSymbol): Boolean
    public abstract fun isDirectSubClassOf(subClass: KaClassOrObjectSymbol, superClass: KaClassOrObjectSymbol): Boolean

    public abstract fun getIntersectionOverriddenSymbols(symbol: KaCallableSymbol): Collection<KaCallableSymbol>
}

public typealias KtSymbolDeclarationOverridesProvider = KaSymbolDeclarationOverridesProvider

public interface KaSymbolDeclarationOverridesProviderMixIn : KaSessionMixIn {
    /**
     * Return a list of **all** explicitly declared symbols that are overridden by symbol
     *
     * E.g., if we have `A.foo` overrides `B.foo` overrides `C.foo`, all two super declarations `B.foo`, `C.foo` will be returned
     *
     * Unwraps substituted overridden symbols
     * (see [INTERSECTION_OVERRIDE][org.jetbrains.kotlin.analysis.api.symbols.KaSymbolOrigin.INTERSECTION_OVERRIDE] and [SUBSTITUTION_OVERRIDE][org.jetbrains.kotlin.analysis.api.symbols.KaSymbolOrigin.SUBSTITUTION_OVERRIDE]),
     * so such fake declaration won't be returned.
     *
     * @see getDirectlyOverriddenSymbols
     */
    public fun KaCallableSymbol.getAllOverriddenSymbols(): List<KaCallableSymbol> =
        withValidityAssertion { analysisSession.symbolDeclarationOverridesProvider.getAllOverriddenSymbols(this) }

    /**
     * Return a list of explicitly declared symbols which are **directly** overridden by symbol
     **
     * E.g., if we have `A.foo` overrides `B.foo` overrides `C.foo`, only declarations directly overridden `B.foo` will be returned
     *
     * Unwraps substituted overridden symbols
     * (see [INTERSECTION_OVERRIDE][org.jetbrains.kotlin.analysis.api.symbols.KaSymbolOrigin.INTERSECTION_OVERRIDE] and [SUBSTITUTION_OVERRIDE][org.jetbrains.kotlin.analysis.api.symbols.KaSymbolOrigin.SUBSTITUTION_OVERRIDE]),
     * so such fake declaration won't be returned.
     *
     *  @see getAllOverriddenSymbols
     */
    public fun KaCallableSymbol.getDirectlyOverriddenSymbols(): List<KaCallableSymbol> =
        withValidityAssertion { analysisSession.symbolDeclarationOverridesProvider.getDirectlyOverriddenSymbols(this) }

    /**
     * Checks if [this] class has [superClass] as its superclass somewhere in the inheritance hierarchy.
     *
     * N.B. The class is not considered to be a subclass of itself, so `myClass.isSubClassOf(myClass)` is always `false`.
     */
    public fun KaClassOrObjectSymbol.isSubClassOf(superClass: KaClassOrObjectSymbol): Boolean =
        withValidityAssertion { analysisSession.symbolDeclarationOverridesProvider.isSubClassOf(this, superClass) }

    /**
     * Checks if [this] class has [superClass] listed as its direct superclass.
     *
     * N.B. The class is not considered to be a direct subclass of itself, so `myClass.isDirectSubClassOf(myClass)` is always `false`.
     */
    public fun KaClassOrObjectSymbol.isDirectSubClassOf(superClass: KaClassOrObjectSymbol): Boolean =
        withValidityAssertion { analysisSession.symbolDeclarationOverridesProvider.isDirectSubClassOf(this, superClass) }

    public fun KaCallableSymbol.getIntersectionOverriddenSymbols(): Collection<KaCallableSymbol> =
        withValidityAssertion { analysisSession.symbolDeclarationOverridesProvider.getIntersectionOverriddenSymbols(this) }
}

public typealias KtSymbolDeclarationOverridesProviderMixIn = KaSymbolDeclarationOverridesProviderMixIn