/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.components

import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.renderer.declarations.KaDeclarationRenderer
import org.jetbrains.kotlin.analysis.api.renderer.declarations.impl.KaDeclarationRendererForSource
import org.jetbrains.kotlin.analysis.api.renderer.types.KaTypeRenderer
import org.jetbrains.kotlin.analysis.api.renderer.types.impl.KaTypeRendererForSource
import org.jetbrains.kotlin.analysis.api.symbols.KaDeclarationSymbol
import org.jetbrains.kotlin.analysis.api.types.KaType
import org.jetbrains.kotlin.types.Variance

public abstract class KaSymbolDeclarationRendererProvider : KaSessionComponent() {
    public abstract fun renderDeclaration(symbol: KaDeclarationSymbol, renderer: KaDeclarationRenderer): String

    public abstract fun renderType(type: KaType, renderer: KaTypeRenderer, position: Variance): String
}

public typealias KtSymbolDeclarationRendererProvider = KaSymbolDeclarationRendererProvider

/**
 * Provides services for rendering Symbols and Types into the Kotlin strings
 */
public interface KaSymbolDeclarationRendererMixIn : KaSessionMixIn {
    /**
     * Render symbol into the representable Kotlin string
     */
    public fun KaDeclarationSymbol.render(renderer: KaDeclarationRenderer = KaDeclarationRendererForSource.WITH_QUALIFIED_NAMES): String =
        withValidityAssertion { analysisSession.symbolDeclarationRendererProvider.renderDeclaration(this, renderer) }

    /**
     * Render kotlin type into the representable Kotlin type string
     */
    public fun KaType.render(
        renderer: KaTypeRenderer = KaTypeRendererForSource.WITH_QUALIFIED_NAMES,
        position: Variance,
    ): String =
        withValidityAssertion { analysisSession.symbolDeclarationRendererProvider.renderType(this, renderer, position) }
}

public typealias KtSymbolDeclarationRendererMixIn = KaSymbolDeclarationRendererMixIn