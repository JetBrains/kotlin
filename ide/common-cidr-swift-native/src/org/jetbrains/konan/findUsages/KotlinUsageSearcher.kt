/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.konan.findUsages

import com.intellij.openapi.application.QueryExecutorBase
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReference
import com.intellij.psi.search.SearchScope
import com.intellij.psi.search.UsageSearchContext
import com.intellij.psi.search.searches.ReferencesSearch.SearchParameters
import com.intellij.util.Processor
import com.jetbrains.cidr.lang.symbols.OCSymbol
import org.jetbrains.konan.resolve.symbols.KtSymbolPsiWrapper

abstract class KotlinUsageSearcher<T : OCSymbol, E> : QueryExecutorBase<PsiReference, SearchParameters>(true) {
    final override fun processQuery(parameters: SearchParameters, consumer: Processor<in PsiReference>) {
        val target = parameters.getTarget() ?: return
        val symbols = target.toLightSymbols()

        var effectiveSearchScope: SearchScope? = null
        for (symbol in symbols) {
            val word = symbol.word ?: continue
            val psiWrapper = createWrapper(target, symbol)
            if (effectiveSearchScope == null) {
                //infer effectiveSearchScope only once. it's the same for all symbols
                val symbolParameters = parameters.duplicateWith(psiWrapper)
                effectiveSearchScope = symbolParameters.effectiveSearchScope
            }
            parameters.optimizer.searchWord(word, effectiveSearchScope, UsageSearchContext.IN_CODE, true, psiWrapper)
        }
    }

    protected abstract fun SearchParameters.getTarget(): E?
    protected abstract fun E.toLightSymbols(): List<T>
    protected abstract fun createWrapper(target: E, symbol: T): KtSymbolPsiWrapper
    protected abstract val T.word: String?
}

internal fun SearchParameters.getUnwrappedTarget(): PsiElement =
    elementToSearch.let { (it as? KtSymbolPsiWrapper)?.psi ?: it }

internal fun SearchParameters.duplicateWith(psi: PsiElement): SearchParameters =
    SearchParameters(psi, scopeDeterminedByUser, isIgnoreAccessScope, optimizer)