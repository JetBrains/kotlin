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
import org.jetbrains.kotlin.idea.completion.LookupElementSink
import org.jetbrains.kotlin.idea.completion.context.FirBasicCompletionContext
import org.jetbrains.kotlin.idea.completion.context.FirRawPositionCompletionContext
import org.jetbrains.kotlin.idea.completion.lookups.CallableImportStrategy
import org.jetbrains.kotlin.idea.completion.lookups.CallableInsertionStrategy
import org.jetbrains.kotlin.idea.completion.lookups.factories.KotlinFirLookupElementFactory
import org.jetbrains.kotlin.idea.completion.weighers.Weighers
import org.jetbrains.kotlin.idea.fir.low.level.api.IndexHelper
import org.jetbrains.kotlin.idea.frontend.api.KtAnalysisSession
import org.jetbrains.kotlin.idea.frontend.api.scopes.KtScopeNameFilter
import org.jetbrains.kotlin.idea.frontend.api.symbols.*
import org.jetbrains.kotlin.idea.frontend.api.symbols.markers.KtNamedSymbol
import org.jetbrains.kotlin.idea.frontend.api.types.KtType
import org.jetbrains.kotlin.idea.references.mainReference
import org.jetbrains.kotlin.platform.TargetPlatform
import org.jetbrains.kotlin.psi.KtDotQualifiedExpression
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtFile

internal abstract class FirCompletionContributorBase<C : FirRawPositionCompletionContext>(protected val basicContext: FirBasicCompletionContext) {
    protected val prefixMatcher: PrefixMatcher get() = basicContext.prefixMatcher
    protected val parameters: CompletionParameters get() = basicContext.parameters
    protected val sink: LookupElementSink get() = basicContext.sink
    protected val originalKtFile: KtFile get() = basicContext.originalKtFile
    protected val fakeKtFile: KtFile get() = basicContext.fakeKtFile
    protected val project: Project get() = basicContext.project
    protected val targetPlatform: TargetPlatform get() = basicContext.targetPlatform
    protected val indexHelper: IndexHelper get() = basicContext.indexHelper
    protected val lookupElementFactory: KotlinFirLookupElementFactory get() = basicContext.lookupElementFactory
    protected val visibleScope = basicContext.visibleScope

    protected val scopeNameFilter: KtScopeNameFilter =
        { name -> !name.isSpecial && prefixMatcher.prefixMatches(name.identifier) }

    abstract fun KtAnalysisSession.complete(positionContext: C)

    protected fun KtAnalysisSession.addSymbolToCompletion(expectedType: KtType?, symbol: KtSymbol) {
        if (symbol !is KtNamedSymbol) return
        with(lookupElementFactory) {
            createLookupElement(symbol)
                .let { applyWeighers(it, symbol, expectedType) }
                .let(sink::addElement)
        }
    }

    protected fun KtAnalysisSession.addClassifierSymbolToCompletion(symbol: KtClassifierSymbol, insertFqName: Boolean) {
        if (symbol !is KtNamedSymbol) return
        val lookup = with(lookupElementFactory) {
            when (symbol) {
                is KtClassLikeSymbol -> createLookupElementForClassLikeSymbol(symbol, insertFqName)
                is KtTypeParameterSymbol -> createLookupElement(symbol)
            }
        } ?: return
        sink.addElement(lookup)
    }

    protected fun KtAnalysisSession.addCallableSymbolToCompletion(
        symbol: KtCallableSymbol,
        importingStrategy: CallableImportStrategy,
        insertionStrategy: CallableInsertionStrategy,
    ) {
        if (symbol !is KtNamedSymbol) return
        val lookup = with(lookupElementFactory) {
            createCallableLookupElement(symbol, importingStrategy, insertionStrategy)
        } ?: return
        sink.addElement(lookup)
    }

    protected fun KtExpression.reference() = when (this) {
        is KtDotQualifiedExpression -> selectorExpression?.mainReference
        else -> mainReference
    }

    private fun KtAnalysisSession.applyWeighers(
        lookupElement: LookupElement,
        symbol: KtSymbol,
        expectedType: KtType?
    ): LookupElement = lookupElement.apply {
        with(Weighers) { applyWeighsToLookupElement(lookupElement, symbol, expectedType) }
    }
}

internal fun <C : FirRawPositionCompletionContext> KtAnalysisSession.complete(
    contextContributor: FirCompletionContributorBase<C>,
    positionContext: C,
) {
    with(contextContributor) {
        complete(positionContext)
    }
}