/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.renderer.declarations.modifiers

import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.renderer.base.KtKeywordsRenderer
import org.jetbrains.kotlin.analysis.api.renderer.declarations.modifiers.renderers.*
import org.jetbrains.kotlin.analysis.api.symbols.KtDeclarationSymbol
import org.jetbrains.kotlin.analysis.utils.printer.PrettyPrinter

public class KtDeclarationModifiersRenderer private constructor(
    public val modifierListRenderer: KtModifierListRenderer,
    public val modifiersSorter: KtModifiersSorter,
    public val modalityProvider: KtRendererModalityModifierProvider,
    public val visibilityProvider: KtRendererVisibilityModifierProvider,
    public val otherModifiersProvider: KtRendererOtherModifiersProvider,
    public val keywordsRenderer: KtKeywordsRenderer,
) {
    context(KtAnalysisSession)
    public fun renderDeclarationModifiers(symbol: KtDeclarationSymbol, printer: PrettyPrinter) {
        modifierListRenderer.renderModifiers(symbol, printer)
    }

    public inline fun with(action: Builder.() -> Unit): KtDeclarationModifiersRenderer {
        val renderer = this
        return KtDeclarationModifiersRenderer {
            this.modifierListRenderer = renderer.modifierListRenderer
            this.modifiersSorter = renderer.modifiersSorter
            this.modalityProvider = renderer.modalityProvider
            this.visibilityProvider = renderer.visibilityProvider
            this.otherModifiersProvider = renderer.otherModifiersProvider
            this.keywordsRenderer = renderer.keywordsRenderer
            action()
        }
    }

    public companion object {
        public inline operator fun invoke(action: Builder.() -> Unit): KtDeclarationModifiersRenderer =
            Builder().apply(action).build()
    }

    public class Builder {
        public lateinit var modifierListRenderer: KtModifierListRenderer
        public lateinit var modifiersSorter: KtModifiersSorter
        public lateinit var modalityProvider: KtRendererModalityModifierProvider
        public lateinit var visibilityProvider: KtRendererVisibilityModifierProvider
        public lateinit var otherModifiersProvider: KtRendererOtherModifiersProvider
        public lateinit var keywordsRenderer: KtKeywordsRenderer

        public fun build(): KtDeclarationModifiersRenderer = KtDeclarationModifiersRenderer(
            modifierListRenderer,
            modifiersSorter,
            modalityProvider,
            visibilityProvider,
            otherModifiersProvider,
            keywordsRenderer,
        )
    }
}