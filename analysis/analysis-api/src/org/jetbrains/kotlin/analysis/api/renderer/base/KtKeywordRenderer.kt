/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.renderer.base

import org.jetbrains.kotlin.analysis.api.KaExperimentalApi
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.annotations.KaAnnotated
import org.jetbrains.kotlin.analysis.utils.printer.PrettyPrinter
import org.jetbrains.kotlin.lexer.KtKeywordToken

@KaExperimentalApi
public interface KaKeywordRenderer {
    public fun renderKeyword(
        analysisSession: KaSession,
        keyword: KtKeywordToken,
        owner: KaAnnotated,
        keywordsRenderer: KaKeywordsRenderer,
        printer: PrettyPrinter,
    )

    public fun renderKeywords(
        analysisSession: KaSession,
        keywords: List<KtKeywordToken>,
        owner: KaAnnotated,
        keywordsRenderer: KaKeywordsRenderer,
        printer: PrettyPrinter,
    ) {
        val applicableKeywords = keywords.filter { keywordsRenderer.keywordFilter.filter(analysisSession, it, owner) }
        printer.printCollection(applicableKeywords, separator = " ") {
            renderKeyword(analysisSession, it, owner, keywordsRenderer, this)
        }
    }

    public object AS_WORD : KaKeywordRenderer {
        override fun renderKeyword(
            analysisSession: KaSession,
            keyword: KtKeywordToken,
            owner: KaAnnotated,
            keywordsRenderer: KaKeywordsRenderer,
            printer: PrettyPrinter,
        ) {
            if (keywordsRenderer.keywordFilter.filter(analysisSession, keyword, owner)) {
                printer.append(keyword.value)
            }
        }
    }

    public object NONE : KaKeywordRenderer {
        override fun renderKeyword(
            analysisSession: KaSession,
            keyword: KtKeywordToken,
            owner: KaAnnotated,
            keywordsRenderer: KaKeywordsRenderer,
            printer: PrettyPrinter,
        ) {}
    }
}

@KaExperimentalApi
@Deprecated("Use 'KaKeywordRenderer' instead", ReplaceWith("KaKeywordRenderer"))
public typealias KtKeywordRenderer = KaKeywordRenderer