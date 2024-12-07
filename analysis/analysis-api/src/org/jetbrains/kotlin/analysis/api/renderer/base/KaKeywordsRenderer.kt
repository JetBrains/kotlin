/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.renderer.base

import org.jetbrains.kotlin.analysis.api.KaExperimentalApi
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.annotations.KaAnnotated
import org.jetbrains.kotlin.analysis.api.renderer.declarations.modifiers.renderers.KaRendererKeywordFilter
import org.jetbrains.kotlin.analysis.utils.printer.PrettyPrinter
import org.jetbrains.kotlin.lexer.KtKeywordToken

@KaExperimentalApi
public class KaKeywordsRenderer private constructor(
    public val keywordRenderer: KaKeywordRenderer,
    public val keywordFilter: KaRendererKeywordFilter,
) {

    public fun renderKeyword(analysisSession: KaSession, keyword: KtKeywordToken, owner: KaAnnotated, printer: PrettyPrinter) {
        keywordRenderer.renderKeyword(analysisSession, keyword, owner, this, printer)
    }

    public fun renderKeywords(analysisSession: KaSession, keywords: List<KtKeywordToken>, owner: KaAnnotated, printer: PrettyPrinter) {
        keywordRenderer.renderKeywords(analysisSession, keywords, owner, this, printer)
    }

    public inline fun with(action: Builder.() -> Unit): KaKeywordsRenderer {
        val renderer = this
        return KaKeywordsRenderer {
            this.keywordRenderer = renderer.keywordRenderer
            this.keywordFilter = renderer.keywordFilter
            action()
        }
    }

    @KaExperimentalApi
    public class Builder {
        public lateinit var keywordRenderer: KaKeywordRenderer
        public lateinit var keywordFilter: KaRendererKeywordFilter

        public fun build(): KaKeywordsRenderer = KaKeywordsRenderer(
            keywordRenderer,
            keywordFilter
        )
    }

    @KaExperimentalApi
    public companion object {
        public val AS_WORD: KaKeywordsRenderer = KaKeywordsRenderer(KaKeywordRenderer.AS_WORD, KaRendererKeywordFilter.ALL)
        public val NONE: KaKeywordsRenderer = KaKeywordsRenderer(KaKeywordRenderer.NONE, KaRendererKeywordFilter.ALL)
        public inline operator fun invoke(action: Builder.() -> Unit): KaKeywordsRenderer =
            Builder().apply(action).build()
    }
}
