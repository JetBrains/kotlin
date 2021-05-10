/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.completion.contributors

import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.completion.PrefixMatcher
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.idea.completion.KotlinFirLookupElementFactory
import org.jetbrains.kotlin.idea.completion.context.FirBasicCompletionContext
import org.jetbrains.kotlin.idea.completion.weighers.Weighers
import org.jetbrains.kotlin.idea.completion.weighers.Weighers.applyWeighsToLookupElement
import org.jetbrains.kotlin.idea.frontend.api.KtAnalysisSession
import org.jetbrains.kotlin.idea.frontend.api.symbols.KtSymbol
import org.jetbrains.kotlin.idea.frontend.api.symbols.markers.KtNamedSymbol
import org.jetbrains.kotlin.idea.frontend.api.types.KtType
import org.jetbrains.kotlin.platform.TargetPlatform
import org.jetbrains.kotlin.psi.KtFile

internal abstract class FirCompletionContributorBase(protected val basicContext: FirBasicCompletionContext) {
    protected val prefixMatcher: PrefixMatcher get() = basicContext.prefixMatcher
    protected val parameters: CompletionParameters get() = basicContext.parameters
    protected val result: CompletionResultSet get() = basicContext.result
    protected val originalKtFile: KtFile get() = basicContext.originalKtFile
    protected val fakeKtFile: KtFile get() = basicContext.fakeKtFile
    protected val project: Project get() = basicContext.project
    protected val targetPlatform: TargetPlatform get() = basicContext.targetPlatform
    protected val lookupElementFactory: KotlinFirLookupElementFactory get() = basicContext.lookupElementFactory

    protected fun KtAnalysisSession.addSymbolToCompletion(expectedType: KtType?, symbol: KtSymbol) {
        if (symbol !is KtNamedSymbol) return
        with(lookupElementFactory) {
            createLookupElement(symbol)
                ?.let { applyWeighers(it, symbol, expectedType) }
                ?.let(result::addElement)
        }
    }

    private fun KtAnalysisSession.applyWeighers(
        lookupElement: LookupElement,
        symbol: KtSymbol,
        expectedType: KtType?
    ): LookupElement = lookupElement.apply {
        with(Weighers) { applyWeighsToLookupElement(lookupElement, symbol, expectedType) }
    }
}