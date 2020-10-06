/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.frontend.api.components

import org.jetbrains.kotlin.idea.frontend.api.symbols.KtCallableSymbol
import org.jetbrains.kotlin.idea.frontend.api.symbols.KtClassOrObjectSymbol
import org.jetbrains.kotlin.idea.frontend.api.symbols.KtFunctionSymbol
import org.jetbrains.kotlin.idea.frontend.api.symbols.KtSymbol

abstract class KtSymbolDeclarationOverridesProvider : KtAnalysisSessionComponent() {
    /**
     * Returns symbols that overridden by requested
     */
    abstract fun <T : KtSymbol> getOverriddenSymbols(
        callableSymbol: T,
        containingDeclaration: KtClassOrObjectSymbol
    ): List<KtCallableSymbol>

    //abstract fun getOverriddenSymbols(callableSymbol: KtCallableSymbol, containingDeclaration: KtClassOrObjectSymbol): List<KtCallableSymbol>
}