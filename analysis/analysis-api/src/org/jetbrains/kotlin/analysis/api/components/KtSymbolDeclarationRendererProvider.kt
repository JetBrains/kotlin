/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.components

import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.renderer.declarations.KtDeclarationRenderer
import org.jetbrains.kotlin.analysis.api.renderer.declarations.impl.KtDeclarationRendererForSource
import org.jetbrains.kotlin.analysis.api.renderer.types.KtTypeRenderer
import org.jetbrains.kotlin.analysis.api.renderer.types.impl.KtTypeRendererForSource
import org.jetbrains.kotlin.analysis.api.symbols.KtDeclarationSymbol
import org.jetbrains.kotlin.analysis.api.types.KtType
import org.jetbrains.kotlin.types.Variance


public abstract class KtSymbolDeclarationRendererProvider : KtAnalysisSessionComponent() {
    public abstract fun renderDeclaration(symbol: KtDeclarationSymbol, renderer: KtDeclarationRenderer): String

    public abstract fun renderType(type: KtType, renderer: KtTypeRenderer, position: Variance): String
}

/**
 * Provides services for rendering Symbols and Types into the Kotlin strings
 */
public interface KtSymbolDeclarationRendererMixIn : KtAnalysisSessionMixIn {
    /**
     * Render symbol into the representable Kotlin string
     */
    public fun KtDeclarationSymbol.render(renderer: KtDeclarationRenderer = KtDeclarationRendererForSource.WITH_QUALIFIED_NAMES): String =
        withValidityAssertion { analysisSession.symbolDeclarationRendererProvider.renderDeclaration(this, renderer) }

    /**
     * Render kotlin type into the representable Kotlin type string
     */
    public fun KtType.render(
        renderer: KtTypeRenderer = KtTypeRendererForSource.WITH_QUALIFIED_NAMES,
        position: Variance,
    ): String =
        withValidityAssertion { analysisSession.symbolDeclarationRendererProvider.renderType(this, renderer, position) }
}