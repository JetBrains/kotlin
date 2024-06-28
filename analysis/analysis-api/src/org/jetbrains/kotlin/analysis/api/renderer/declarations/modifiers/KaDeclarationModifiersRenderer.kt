/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.renderer.declarations.modifiers

import org.jetbrains.kotlin.analysis.api.KaExperimentalApi
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.renderer.base.KaKeywordsRenderer
import org.jetbrains.kotlin.analysis.api.renderer.declarations.modifiers.renderers.*
import org.jetbrains.kotlin.analysis.api.symbols.KaDeclarationSymbol
import org.jetbrains.kotlin.analysis.utils.printer.PrettyPrinter

@KaExperimentalApi
public class KaDeclarationModifiersRenderer private constructor(
    public val modifierListRenderer: KaModifierListRenderer,
    public val modifiersSorter: KaModifiersSorter,
    public val modalityProvider: KaRendererModalityModifierProvider,
    public val visibilityProvider: KaRendererVisibilityModifierProvider,
    public val otherModifiersProvider: KaRendererOtherModifiersProvider,
    public val keywordsRenderer: KaKeywordsRenderer,
) {
    public fun renderDeclarationModifiers(analysisSession: KaSession, symbol: KaDeclarationSymbol, printer: PrettyPrinter) {
        modifierListRenderer.renderModifiers(analysisSession, symbol, this, printer)
    }

    public inline fun with(action: Builder.() -> Unit): KaDeclarationModifiersRenderer {
        val renderer = this
        return KaDeclarationModifiersRenderer {
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
        public inline operator fun invoke(action: Builder.() -> Unit): KaDeclarationModifiersRenderer =
            Builder().apply(action).build()
    }

    public class Builder {
        public lateinit var modifierListRenderer: KaModifierListRenderer
        public lateinit var modifiersSorter: KaModifiersSorter
        public lateinit var modalityProvider: KaRendererModalityModifierProvider
        public lateinit var visibilityProvider: KaRendererVisibilityModifierProvider
        public lateinit var otherModifiersProvider: KaRendererOtherModifiersProvider
        public lateinit var keywordsRenderer: KaKeywordsRenderer

        public fun build(): KaDeclarationModifiersRenderer = KaDeclarationModifiersRenderer(
            modifierListRenderer,
            modifiersSorter,
            modalityProvider,
            visibilityProvider,
            otherModifiersProvider,
            keywordsRenderer,
        )
    }
}

@KaExperimentalApi
@Deprecated("Use 'KaDeclarationModifiersRenderer' instead", ReplaceWith("KaDeclarationModifiersRenderer"))
public typealias KtDeclarationModifiersRenderer = KaDeclarationModifiersRenderer