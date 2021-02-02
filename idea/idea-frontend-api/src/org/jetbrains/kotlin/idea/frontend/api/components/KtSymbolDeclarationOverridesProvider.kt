/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.frontend.api.components

import org.jetbrains.kotlin.idea.frontend.api.symbols.KtCallableSymbol
import org.jetbrains.kotlin.idea.frontend.api.symbols.KtClassOrObjectSymbol
import org.jetbrains.kotlin.idea.frontend.api.symbols.KtSymbol

abstract class KtSymbolDeclarationOverridesProvider : KtAnalysisSessionComponent() {
    /**
     * Returns symbols that overridden by requested
     */
    abstract fun <T : KtSymbol> getOverriddenSymbols(
        callableSymbol: T,
        containingDeclaration: KtClassOrObjectSymbol
    ): List<KtCallableSymbol>

    /**
     * Returns symbols that overridden by requested
     */
    abstract fun <T : KtSymbol> getOverriddenSymbols(
        callableSymbol: T,
    ): List<KtCallableSymbol>

    /**
     * If [symbol] origin is [org.jetbrains.kotlin.idea.frontend.api.symbols.KtSymbolOrigin.INTERSECTION_OVERRIDE]
     * Then returns the symbols which [symbol] overrides, otherwise empty collection
     */
    abstract fun getIntersectionOverriddenSymbols(symbol: KtCallableSymbol): Collection<KtCallableSymbol>
}