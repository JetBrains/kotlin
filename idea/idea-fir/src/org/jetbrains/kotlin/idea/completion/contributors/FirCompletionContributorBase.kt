/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.completion.contributors

import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.PrefixMatcher
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.idea.completion.LookupElementSink
import org.jetbrains.kotlin.idea.completion.context.FirBasicCompletionContext
import org.jetbrains.kotlin.idea.completion.context.FirRawPositionCompletionContext
import org.jetbrains.kotlin.idea.completion.lookups.*
import org.jetbrains.kotlin.idea.completion.lookups.CallableInsertionOptions
import org.jetbrains.kotlin.idea.completion.lookups.ImportStrategy
import org.jetbrains.kotlin.idea.completion.lookups.detectImportStrategy
import org.jetbrains.kotlin.idea.completion.lookups.factories.KotlinFirLookupElementFactory
import org.jetbrains.kotlin.idea.completion.weighers.Weighers
import org.jetbrains.kotlin.idea.fir.HLIndexHelper
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

internal class FirCompletionContributorOptions(
    val priority: Int = 0
) {
    companion object {
        val DEFAULT = FirCompletionContributorOptions()
    }
}

internal abstract class FirCompletionContributorBase<C : FirRawPositionCompletionContext>(
    protected val basicContext: FirBasicCompletionContext,
    options: FirCompletionContributorOptions,
) {

    constructor(basicContext: FirBasicCompletionContext, priority: Int) :
            this(basicContext, FirCompletionContributorOptions(priority))

    protected val prefixMatcher: PrefixMatcher get() = basicContext.prefixMatcher
    protected val parameters: CompletionParameters get() = basicContext.parameters
    protected val sink: LookupElementSink = basicContext.sink.withPriority(options.priority)
    protected val originalKtFile: KtFile get() = basicContext.originalKtFile
    protected val fakeKtFile: KtFile get() = basicContext.fakeKtFile
    protected val project: Project get() = basicContext.project
    protected val targetPlatform: TargetPlatform get() = basicContext.targetPlatform
    protected val indexHelper: HLIndexHelper get() = basicContext.indexHelper
    protected val lookupElementFactory: KotlinFirLookupElementFactory get() = basicContext.lookupElementFactory
    protected val visibleScope = basicContext.visibleScope


    protected val scopeNameFilter: KtScopeNameFilter =
        { name -> !name.isSpecial && prefixMatcher.prefixMatches(name.identifier) }

    abstract fun KtAnalysisSession.complete(positionContext: C)

    protected fun KtAnalysisSession.addSymbolToCompletion(expectedType: KtType?, symbol: KtSymbol) {
        if (symbol !is KtNamedSymbol) return
        with(lookupElementFactory) {
            createLookupElement(symbol)
                .let(sink::addElement)
        }
    }

    protected fun KtAnalysisSession.addClassifierSymbolToCompletion(
        symbol: KtClassifierSymbol,
        importingStrategy: ImportStrategy = detectImportStrategy(symbol)
    ) {
        if (symbol !is KtNamedSymbol) return
        val lookup = with(lookupElementFactory) {
            when (symbol) {
                is KtClassLikeSymbol -> createLookupElementForClassLikeSymbol(symbol, importingStrategy)
                is KtTypeParameterSymbol -> createLookupElement(symbol)
            }
        } ?: return
        sink.addElement(lookup)
    }

    protected fun KtAnalysisSession.addCallableSymbolToCompletion(
        expectedType: KtType?,
        symbol: KtCallableSymbol,
        options: CallableInsertionOptions,
    ) {
        if (symbol !is KtNamedSymbol) return
        val lookup = with(lookupElementFactory) {
            createCallableLookupElement(symbol, options)
        }
        applyWeighers(lookup, symbol, expectedType)
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