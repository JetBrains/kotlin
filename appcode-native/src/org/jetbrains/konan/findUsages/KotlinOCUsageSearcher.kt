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
import com.intellij.psi.search.searches.ReferencesSearch
import com.intellij.util.Processor
import com.jetbrains.cidr.lang.symbols.OCSymbol
import org.jetbrains.konan.resolve.symbols.KtOCLightSymbol
import org.jetbrains.konan.resolve.symbols.KtOCPsiWrapper
import org.jetbrains.konan.resolve.findSymbols
import org.jetbrains.kotlin.psi.KtNamedDeclaration

class KotlinOCUsageSearcher : QueryExecutorBase<PsiReference, ReferencesSearch.SearchParameters>(true) {
    override fun processQuery(parameters: ReferencesSearch.SearchParameters, consumer: Processor<in PsiReference>) {
        val target = parameters.getUnwrappedTarget() as? KtNamedDeclaration ?: return
        val symbols = target.toLightSymbols()
        val optimizer = parameters.optimizer
        var effectiveSearchScope: SearchScope? = null
        symbols.forEach { symbol ->
            val psiWrapper = KtOCPsiWrapper(target, symbol)
            if (effectiveSearchScope == null) {
                //infer effectiveSearchScope only once. it's the same for all symbols
                val symbolParameters = parameters.duplicateWith(psiWrapper)
                effectiveSearchScope = symbolParameters.effectiveSearchScope
            }
            optimizer.searchWord(symbol.name, effectiveSearchScope!!, UsageSearchContext.IN_CODE, true, psiWrapper)
        }
    }
}

internal fun ReferencesSearch.SearchParameters.getUnwrappedTarget(): PsiElement {
    val elementToSearch = elementToSearch
    return (elementToSearch as? KtOCPsiWrapper)?.psi ?: elementToSearch
}

internal fun ReferencesSearch.SearchParameters.duplicateWith(psi: PsiElement): ReferencesSearch.SearchParameters {
    return ReferencesSearch.SearchParameters(psi, scopeDeterminedByUser, isIgnoreAccessScope, optimizer)
}

internal fun KtNamedDeclaration.toLightSymbols(): List<OCSymbol> = findSymbols().map { symbol -> KtOCLightSymbol(this, symbol) }