/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.completion.lookups

import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.openapi.util.TextRange
import org.jetbrains.kotlin.idea.completion.KotlinFirIconProvider
import org.jetbrains.kotlin.idea.frontend.api.KtAnalysisSession
import org.jetbrains.kotlin.idea.frontend.api.analyse
import org.jetbrains.kotlin.idea.frontend.api.symbols.KtSymbol
import org.jetbrains.kotlin.idea.frontend.api.tokens.HackToForceAllowRunningAnalyzeOnEDT
import org.jetbrains.kotlin.idea.frontend.api.tokens.hackyAllowRunningOnEdt
import org.jetbrains.kotlin.psi.KtFile

internal fun KtAnalysisSession.withSymbolInfo(
    symbol: KtSymbol,
    elementBuilder: LookupElementBuilder
): LookupElementBuilder = elementBuilder
    .withPsiElement(symbol.psi) // TODO check if it is a heavy operation and should be postponed
    .withIcon(KotlinFirIconProvider.getIconFor(symbol))


// FIXME: This is a hack, we should think how we can get rid of it
internal inline fun <T> withAllowedResolve(action: () -> T): T {
    @OptIn(HackToForceAllowRunningAnalyzeOnEDT::class)
    return hackyAllowRunningOnEdt(action)
}

internal fun CharSequence.skipSpaces(index: Int): Int =
    (index until length).firstOrNull { val c = this[it]; c != ' ' && c != '\t' } ?: this.length

internal fun CharSequence.indexOfSkippingSpace(c: Char, startIndex: Int): Int? {
    for (i in startIndex until this.length) {
        val currentChar = this[i]
        if (c == currentChar) return i
        if (currentChar != ' ' && currentChar != '\t') return null
    }
    return null
}

internal fun shortenReferencesForFirCompletion(targetFile: KtFile, textRange: TextRange) {
    val shortenings = withAllowedResolve {
        analyse(targetFile) {
            collectPossibleReferenceShortenings(targetFile, textRange)
        }
    }
    shortenings.invokeShortening()
}

