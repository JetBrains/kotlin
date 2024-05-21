/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.components

import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.util.ImplementationStatus
import org.jetbrains.kotlin.analysis.api.symbols.KaCallableSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaClassOrObjectSymbol

public abstract class KaOverrideInfoProvider : KaSessionComponent() {
    public abstract fun isVisible(memberSymbol: KaCallableSymbol, classSymbol: KaClassOrObjectSymbol): Boolean
    public abstract fun getImplementationStatus(
        memberSymbol: KaCallableSymbol,
        parentClassSymbol: KaClassOrObjectSymbol
    ): ImplementationStatus?

    public abstract fun unwrapFakeOverrides(symbol: KaCallableSymbol): KaCallableSymbol
    public abstract fun getOriginalContainingClassForOverride(symbol: KaCallableSymbol): KaClassOrObjectSymbol?
}

public typealias KtOverrideInfoProvider = KaOverrideInfoProvider

public interface KaMemberSymbolProviderMixin : KaSessionMixIn {

    /** Checks if the given symbol (possibly a symbol inherited from a super class) is visible in the given class. */
    public fun KaCallableSymbol.isVisibleInClass(classSymbol: KaClassOrObjectSymbol): Boolean =
        withValidityAssertion { analysisSession.overrideInfoProvider.isVisible(this, classSymbol) }

    /**
     * Gets the [ImplementationStatus] of the [this] member symbol in the given [parentClassSymbol]. Or null if this symbol is not a
     * member.
     */
    public fun KaCallableSymbol.getImplementationStatus(parentClassSymbol: KaClassOrObjectSymbol): ImplementationStatus? =
        withValidityAssertion { analysisSession.overrideInfoProvider.getImplementationStatus(this, parentClassSymbol) }

    /**
     * Unwraps fake override [KaCallableSymbol]s until an original declared symbol is uncovered.
     *
     * In a class scope, a symbol may be derived from symbols declared in super classes. For example, consider
     *
     * ```
     * public interface  A<T> {
     *   public fun foo(t:T)
     * }
     *
     * public interface  B: A<String> {
     * }
     * ```
     *
     * In the class scope of `B`, there is a callable symbol `foo` that takes a `String`. This symbol is derived from the original symbol
     * in `A` that takes the type parameter `T` (fake override). Given such a fake override symbol, [unwrapFakeOverrides] recovers the
     * original declared symbol.
     *
     * Such situation can also happen for intersection symbols (in case of multiple super types containing symbols with identical signature
     * after specialization) and delegation.
     */
    public val KaCallableSymbol.unwrapFakeOverrides: KaCallableSymbol
        get() = withValidityAssertion { analysisSession.overrideInfoProvider.unwrapFakeOverrides(this) }

    /**
     * Gets the class symbol where the given callable symbol is declared. See [unwrapFakeOverrides] for more details.
     */
    public val KaCallableSymbol.originalContainingClassForOverride: KaClassOrObjectSymbol?
        get() = withValidityAssertion { analysisSession.overrideInfoProvider.getOriginalContainingClassForOverride(this) }
}

public typealias KtMemberSymbolProviderMixin = KaMemberSymbolProviderMixin