/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.components

import org.jetbrains.kotlin.analysis.api.symbols.KtSymbol
import org.jetbrains.kotlin.analysis.api.symbols.markers.KtSymbolWithKind

public abstract class KtSymbolContainingDeclarationProvider : KtAnalysisSessionComponent() {
    public abstract fun getContainingDeclaration(symbol: KtSymbol): KtSymbolWithKind?
}

public interface KtSymbolContainingDeclarationProviderMixIn : KtAnalysisSessionMixIn {
    /**
     * Returns containing declaration for symbol:
     *   for top-level declarations returns null
     *   for class members returns containing class
     *   for local declaration returns declaration it was declared it
     */
    public fun KtSymbol.getContainingSymbol(): KtSymbolWithKind? =
        analysisSession.containingDeclarationProvider.getContainingDeclaration(this)
}