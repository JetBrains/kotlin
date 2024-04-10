/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.renderer.base

import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.annotations.KtAnnotated
import org.jetbrains.kotlin.analysis.utils.printer.PrettyPrinter
import org.jetbrains.kotlin.lexer.KtKeywordToken

public interface KtKeywordRenderer {
    public fun renderKeyword(
        analysisSession: KtAnalysisSession,
        keyword: KtKeywordToken,
        owner: KtAnnotated,
        keywordsRenderer: KtKeywordsRenderer,
        printer: PrettyPrinter,
    )

    public fun renderKeywords(
        analysisSession: KtAnalysisSession,
        keywords: List<KtKeywordToken>,
        owner: KtAnnotated,
        keywordsRenderer: KtKeywordsRenderer,
        printer: PrettyPrinter,
    ) {
        val applicableKeywords = keywords.filter { keywordsRenderer.keywordFilter.filter(analysisSession, it, owner) }
        printer.printCollection(applicableKeywords, separator = " ") {
            renderKeyword(analysisSession, it, owner, keywordsRenderer, this)
        }
    }

    public object AS_WORD : KtKeywordRenderer {
        override fun renderKeyword(
            analysisSession: KtAnalysisSession,
            keyword: KtKeywordToken,
            owner: KtAnnotated,
            keywordsRenderer: KtKeywordsRenderer,
            printer: PrettyPrinter,
        ) {
            if (keywordsRenderer.keywordFilter.filter(analysisSession, keyword, owner)) {
                printer.append(keyword.value)
            }
        }
    }

    public object NONE : KtKeywordRenderer {
        override fun renderKeyword(
            analysisSession: KtAnalysisSession,
            keyword: KtKeywordToken,
            owner: KtAnnotated,
            keywordsRenderer: KtKeywordsRenderer,
            printer: PrettyPrinter,
        ) {}
    }
}

