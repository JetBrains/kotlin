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
    context(KtAnalysisSession)
    public fun renderKeyword(keyword: KtKeywordToken, owner: KtAnnotated, printer: PrettyPrinter)

    context(KtAnalysisSession)
    public fun renderKeywords(keywords: List<KtKeywordToken>, owner: KtAnnotated, printer: PrettyPrinter) {
        printer.printCollection(keywords, separator = " ") {
            renderKeyword(it, owner, this)
        }
    }

    public object AS_WORD : KtKeywordRenderer {
        context(KtAnalysisSession)
        override fun renderKeyword(keyword: KtKeywordToken, owner: KtAnnotated, printer: PrettyPrinter) {
            printer.append(keyword.value)
        }
    }
}

