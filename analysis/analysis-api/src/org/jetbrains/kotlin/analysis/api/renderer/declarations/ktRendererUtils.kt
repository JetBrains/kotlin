/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.renderer.declarations

import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.annotations.KtAnnotated
import org.jetbrains.kotlin.analysis.api.base.KtContextReceiversOwner
import org.jetbrains.kotlin.analysis.api.symbols.KtDeclarationSymbol
import org.jetbrains.kotlin.analysis.utils.printer.PrettyPrinter
import org.jetbrains.kotlin.lexer.KtKeywordToken

context(KtAnalysisSession, KtDeclarationRenderer)
public fun <S> renderAnnotationsModifiersAndContextReceivers(
    symbol: S,
    printer: PrettyPrinter,
    keyword: KtKeywordToken,
): Unit where S : KtAnnotated, S : KtDeclarationSymbol = printer {
    renderAnnotationsModifiersAndContextReceivers(symbol, printer, listOf(keyword))
}

context(KtAnalysisSession, KtDeclarationRenderer)
public fun <S> renderAnnotationsModifiersAndContextReceivers(
    symbol: S,
    printer: PrettyPrinter,
    keywords: List<KtKeywordToken>,
): Unit where S : KtAnnotated, S : KtDeclarationSymbol = printer {
    renderContextReceivers(symbol, printer)

    val annotationsRendered: Boolean
    val modifiersRendered: Boolean
    codeStyle.getSeparatorBetweenAnnotationAndOwner(symbol).separated(
        { annotationsRendered = checkIfPrinted { annotationRenderer.renderAnnotations(symbol, printer) } },
        { modifiersRendered = checkIfPrinted { modifiersRenderer.renderDeclarationModifiers(symbol, printer) } }
    )
    val separator = when {
        annotationsRendered && !modifiersRendered -> codeStyle.getSeparatorBetweenAnnotationAndOwner(symbol)
        annotationsRendered || modifiersRendered -> codeStyle.getSeparatorBetweenModifiers()
        else -> ""
    }

    withPrefix(separator) {
        keywordsRenderer.renderKeywords(keywords, symbol, printer)
    }
}

context(KtAnalysisSession, KtDeclarationRenderer)
public fun <S> renderAnnotationsModifiersAndContextReceivers(
    symbol: S,
    printer: PrettyPrinter,
): Unit where S : KtAnnotated, S : KtDeclarationSymbol = printer {
    renderContextReceivers(symbol, printer)
    codeStyle.getSeparatorBetweenAnnotationAndOwner(symbol).separated(
        { annotationRenderer.renderAnnotations(symbol, printer) },
        { modifiersRenderer.renderDeclarationModifiers(symbol, printer) }
    )
}

context(KtAnalysisSession, KtDeclarationRenderer)
private fun renderContextReceivers(symbol: KtDeclarationSymbol, printer: PrettyPrinter): Unit = printer {
    if (symbol !is KtContextReceiversOwner) return

    with(typeRenderer) {
        withSuffix(codeStyle.getSeparatorAfterContextReceivers()) {
            contextReceiversRenderer.renderContextReceivers(symbol, printer)
        }
    }
}
