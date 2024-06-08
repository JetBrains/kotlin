/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.components

import org.jetbrains.kotlin.analysis.api.symbols.KaCallableSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaClassLikeSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaClassOrObjectSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaDeclarationSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaFileSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaSamConstructorSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaSymbol
import org.jetbrains.kotlin.analysis.project.structure.KtModule

public interface KaSymbolRelationProvider {
    /**
     * Returns containing declaration for symbol:
     *   for top-level declarations returns null
     *   for class members returns containing class
     *   for local declaration returns declaration it was declared it
     */
    public val KaSymbol.containingSymbol: KaDeclarationSymbol?

    /**
     * Returns containing [KtFile] as [KaFileSymbol]
     *
     * Caveat: returns `null` if the given symbol is already [KaFileSymbol], since there is no containing file.
     *  Similarly, no containing file for libraries and Java, hence `null`.
     */
    public val KaSymbol.containingFile: KaFileSymbol?

    public val KaSymbol.containingModule: KtModule

    /**
     * Returns [KaSamConstructorSymbol] if the given [KaClassLikeSymbol] is a functional interface type, a.k.a. SAM.
     */
    public val KaClassLikeSymbol.samConstructor: KaSamConstructorSymbol?

    /**
     * Return a list of **all** explicitly declared symbols that are overridden by symbol
     *
     * E.g., if we have `A.foo` overrides `B.foo` overrides `C.foo`, all two super declarations `B.foo`, `C.foo` will be returned
     *
     * Unwraps substituted overridden symbols
     * (see [INTERSECTION_OVERRIDE][org.jetbrains.kotlin.analysis.api.symbols.KaSymbolOrigin.INTERSECTION_OVERRIDE] and
     * [SUBSTITUTION_OVERRIDE][org.jetbrains.kotlin.analysis.api.symbols.KaSymbolOrigin.SUBSTITUTION_OVERRIDE]),
     * so such fake declaration won't be returned.
     *
     * @see directlyOverriddenSymbols
     */
    public val KaCallableSymbol.allOverriddenSymbols: Sequence<KaCallableSymbol>

    /**
     * Return a list of explicitly declared symbols which are **directly** overridden by symbol
     **
     * E.g., if we have `A.foo` overrides `B.foo` overrides `C.foo`, only declarations directly overridden `B.foo` will be returned
     *
     * Unwraps substituted overridden symbols
     * (see [INTERSECTION_OVERRIDE][org.jetbrains.kotlin.analysis.api.symbols.KaSymbolOrigin.INTERSECTION_OVERRIDE] and
     * [SUBSTITUTION_OVERRIDE][org.jetbrains.kotlin.analysis.api.symbols.KaSymbolOrigin.SUBSTITUTION_OVERRIDE]),
     * so such fake declaration won't be returned.
     *
     *  @see allOverriddenSymbols
     */
    public val KaCallableSymbol.directlyOverriddenSymbols: Sequence<KaCallableSymbol>

    /**
     * Checks if [this] class has [superClass] as its superclass somewhere in the inheritance hierarchy.
     *
     * N.B. The class is not considered to be a subclass of itself, so `myClass.isSubClassOf(myClass)` is always `false`.
     */
    public fun KaClassOrObjectSymbol.isSubClassOf(superClass: KaClassOrObjectSymbol): Boolean

    /**
     * Checks if [this] class has [superClass] listed as its direct superclass.
     *
     * N.B. The class is not considered to be a direct subclass of itself, so `myClass.isDirectSubClassOf(myClass)` is always `false`.
     */
    public fun KaClassOrObjectSymbol.isDirectSubClassOf(superClass: KaClassOrObjectSymbol): Boolean

    public val KaCallableSymbol.intersectionOverriddenSymbols: List<KaCallableSymbol>
}