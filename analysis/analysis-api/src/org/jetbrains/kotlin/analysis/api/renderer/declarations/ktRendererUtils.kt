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

public fun <S> renderAnnotationsModifiersAndContextReceivers(
    analysisSession: KtAnalysisSession,
    symbol: S,
    declarationRenderer: KtDeclarationRenderer,
    printer: PrettyPrinter,
    keyword: KtKeywordToken,
): Unit where S : KtAnnotated, S : KtDeclarationSymbol = printer {
    renderAnnotationsModifiersAndContextReceivers(analysisSession, symbol, declarationRenderer, printer, listOf(keyword))
}

public fun <S> renderAnnotationsModifiersAndContextReceivers(
    analysisSession: KtAnalysisSession,
    symbol: S,
    declarationRenderer: KtDeclarationRenderer,
    printer: PrettyPrinter,
    keywords: List<KtKeywordToken>,
) where S : KtAnnotated, S : KtDeclarationSymbol {
    printer {
        renderContextReceivers(analysisSession, symbol, declarationRenderer, printer)

        val annotationsRendered: Boolean
        val modifiersRendered: Boolean
        declarationRenderer.codeStyle.getSeparatorBetweenAnnotationAndOwner(analysisSession, symbol).separated(
            { annotationsRendered = checkIfPrinted { declarationRenderer.annotationRenderer.renderAnnotations(analysisSession, symbol, printer) } },
            { modifiersRendered = checkIfPrinted { declarationRenderer.modifiersRenderer.renderDeclarationModifiers(analysisSession, symbol, printer) } }
        )
        val separator = when {
            annotationsRendered && !modifiersRendered -> declarationRenderer.codeStyle.getSeparatorBetweenAnnotationAndOwner(analysisSession, symbol)
            annotationsRendered || modifiersRendered -> declarationRenderer.codeStyle.getSeparatorBetweenModifiers(analysisSession)
            else -> ""
        }

        withPrefix(separator) {
            declarationRenderer.keywordsRenderer.renderKeywords(analysisSession, keywords, symbol, printer)
        }
    }
}

public fun <S> renderAnnotationsModifiersAndContextReceivers(
    analysisSession: KtAnalysisSession,
    symbol: S,
    declarationRenderer: KtDeclarationRenderer,
    printer: PrettyPrinter,
): Unit where S : KtAnnotated, S : KtDeclarationSymbol = printer {
    renderContextReceivers(analysisSession, symbol, declarationRenderer, printer)
    declarationRenderer.codeStyle.getSeparatorBetweenAnnotationAndOwner(analysisSession, symbol).separated(
        { declarationRenderer.annotationRenderer.renderAnnotations(analysisSession, symbol, printer) },
        { declarationRenderer.modifiersRenderer.renderDeclarationModifiers(analysisSession, symbol, printer) }
    )
}

private fun renderContextReceivers(
    analysisSession: KtAnalysisSession,
    symbol: KtDeclarationSymbol,
    declarationRenderer: KtDeclarationRenderer,
    printer: PrettyPrinter,
): Unit = printer {
    if (symbol !is KtContextReceiversOwner) return

    with(declarationRenderer.typeRenderer) {
        withSuffix(declarationRenderer.codeStyle.getSeparatorAfterContextReceivers(analysisSession)) {
            contextReceiversRenderer.renderContextReceivers(analysisSession, symbol, declarationRenderer.typeRenderer, printer)
        }
    }
}
