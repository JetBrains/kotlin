/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.renderer.base

import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.annotations.KtAnnotated
import org.jetbrains.kotlin.analysis.api.renderer.declarations.modifiers.renderers.KtRendererKeywordFilter
import org.jetbrains.kotlin.analysis.utils.printer.PrettyPrinter
import org.jetbrains.kotlin.lexer.KtKeywordToken

public class KtKeywordsRenderer private constructor(
    public val keywordRenderer: KtKeywordRenderer,
    public val keywordFilter: KtRendererKeywordFilter,
) {

    context(KtAnalysisSession)
    public fun renderKeyword(keyword: KtKeywordToken, owner: KtAnnotated, printer: PrettyPrinter) {
        keywordRenderer.renderKeyword(keyword, owner, printer)
    }

    context(KtAnalysisSession)
    public fun renderKeywords(keywords: List<KtKeywordToken>, owner: KtAnnotated, printer: PrettyPrinter) {
        keywordRenderer.renderKeywords(keywords, owner, printer)
    }

    public inline fun with(action: Builder.() -> Unit): KtKeywordsRenderer {
        val renderer = this
        return KtKeywordsRenderer {
            this.keywordRenderer = renderer.keywordRenderer
            this.keywordFilter = renderer.keywordFilter
            action()
        }
    }

    public class Builder {
        public lateinit var keywordRenderer: KtKeywordRenderer
        public lateinit var keywordFilter: KtRendererKeywordFilter

        public fun build(): KtKeywordsRenderer = KtKeywordsRenderer(
            keywordRenderer,
            keywordFilter
        )
    }

    public companion object {
        public val AS_WORD: KtKeywordsRenderer = KtKeywordsRenderer(KtKeywordRenderer.AS_WORD, KtRendererKeywordFilter.ALL)
        public val NONE: KtKeywordsRenderer = KtKeywordsRenderer(KtKeywordRenderer.NONE, KtRendererKeywordFilter.ALL)
        public inline operator fun invoke(action: Builder.() -> Unit): KtKeywordsRenderer =
            Builder().apply(action).build()
    }
}