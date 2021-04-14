/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.frontend.api.components

import org.jetbrains.kotlin.util.ImplementationStatus
import org.jetbrains.kotlin.idea.frontend.api.symbols.KtCallableSymbol
import org.jetbrains.kotlin.idea.frontend.api.symbols.KtClassOrObjectSymbol

abstract class KtOverrideInfoProvider : KtAnalysisSessionComponent() {
    abstract fun isVisible(memberSymbol: KtCallableSymbol, classSymbol: KtClassOrObjectSymbol): Boolean
    abstract fun getImplementationStatus(memberSymbol: KtCallableSymbol, parentClassSymbol: KtClassOrObjectSymbol): ImplementationStatus?
    abstract fun getOriginalOverriddenSymbol(symbol: KtCallableSymbol): KtCallableSymbol?
    abstract fun getOriginalContainingClassForOverride(symbol: KtCallableSymbol): KtClassOrObjectSymbol?
}

interface KtMemberSymbolProviderMixin : KtAnalysisSessionMixIn {

    /** Checks if the given symbol (possibly a symbol inherited from a super class) is visible in the given class. */
    fun KtCallableSymbol.isVisibleInClass(classSymbol: KtClassOrObjectSymbol): Boolean =
        analysisSession.overrideInfoProvider.isVisible(this, classSymbol)

    /**
     * Gets the [ImplementationStatus] of the [this] member symbol in the given [parentClassSymbol]. Or null if this symbol is not a
     * member.
     */
    fun KtCallableSymbol.getImplementationStatus(parentClassSymbol: KtClassOrObjectSymbol): ImplementationStatus? =
        analysisSession.overrideInfoProvider.getImplementationStatus(this, parentClassSymbol)

    /**
     * Gets the original symbol for the given callable symbol. In a class scope, a symbol may be derived from symbols declared in super
     * classes. For example, consider
     *
     * ```
     * interface A<T> {
     *   fun foo(t:T)
     * }
     *
     * interface B: A<String> {
     * }
     * ```
     *
     * In the class scope of `B`, there is a callable symbol `foo` that takes a `String`. This symbol is derived from the original symbol
     * in `A` that takes the type parameter `T`. Given such a derived symbol, [originalOverriddenSymbol] recovers the original declared
     * symbol.
     *
     * Such situation can also happens for intersection symbols (in case of multiple super types containing symbols with identical signature
     * after specialization) and delegation.
     */
    val KtCallableSymbol.originalOverriddenSymbol: KtCallableSymbol?
        get() = analysisSession.overrideInfoProvider.getOriginalOverriddenSymbol(this)

    /**
     * Gets the class symbol where the given callable symbol is declared. See [originalOverriddenSymbol] for more details.
     */
    val KtCallableSymbol.originalContainingClassForOverride: KtClassOrObjectSymbol?
        get() = analysisSession.overrideInfoProvider.getOriginalContainingClassForOverride(this)
}
