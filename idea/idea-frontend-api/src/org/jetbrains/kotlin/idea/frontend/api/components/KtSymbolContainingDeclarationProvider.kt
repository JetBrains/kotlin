/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.frontend.api.components

import org.jetbrains.kotlin.idea.frontend.api.symbols.markers.KtSymbolWithKind

public abstract class KtSymbolContainingDeclarationProvider : KtAnalysisSessionComponent() {
    public abstract fun getContainingDeclaration(symbol: KtSymbolWithKind): KtSymbolWithKind?
}

public interface KtSymbolContainingDeclarationProviderMixIn : KtAnalysisSessionMixIn {
    /**
     * Returns containing declaration for symbol:
     *   for top-level declarations returns null
     *   for class members returns containing class
     *   for local declaration returns declaration it was declared it
     */
    public fun KtSymbolWithKind.getContainingSymbol(): KtSymbolWithKind? =
        analysisSession.containingDeclarationProvider.getContainingDeclaration(this)
}