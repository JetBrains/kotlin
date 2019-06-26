/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.konan.findUsages

import com.intellij.psi.PsiReference
import com.intellij.psi.search.searches.ReferencesSearch
import com.intellij.util.Processor
import com.intellij.util.QueryExecutor
import com.jetbrains.cidr.lang.refactoring.OCNameSuggester
import com.jetbrains.cidr.lang.search.OCMethodReferencesSearch.processRefs
import com.jetbrains.cidr.lang.symbols.objc.OCMethodSymbol
import com.jetbrains.cidr.lang.types.OCObjectType
import org.jetbrains.konan.resolve.symbols.KotlinOCPsiWrapper
import org.jetbrains.konan.resolve.findSymbols
import org.jetbrains.kotlin.idea.debugger.readAction
import org.jetbrains.kotlin.psi.KtFunction

class KotlinFunctionOCUsagesSearcher : QueryExecutor<PsiReference, ReferencesSearch.SearchParameters> {
    override fun execute(parameters: ReferencesSearch.SearchParameters, consumer: Processor<in PsiReference>): Boolean =
        readAction { doExecute(parameters, consumer) }

    private fun doExecute(parameters: ReferencesSearch.SearchParameters, consumer: Processor<in PsiReference>): Boolean {
        val function = parameters.getUnwrappedTarget() as? KtFunction ?: return true
        val symbols = function.findSymbols()
        symbols.forEach { symbol ->
            if (symbol is OCMethodSymbol) {
                if (!processSymbol(function, symbol, parameters, consumer)) {
                    return false
                }
            }
        }
        return true
    }

    private fun processSymbol(
        function: KtFunction,
        symbol: OCMethodSymbol,
        parameters: ReferencesSearch.SearchParameters,
        consumer: Processor<in PsiReference>
    ): Boolean {
        val psiWrapper = KotlinOCPsiWrapper(function, symbol)
        val ocQueryParameters = parameters.duplicateWith(psiWrapper)

        val methodSelector = psiWrapper.symbol.name
        val searchWord = methodSelector.split(":").maxBy { it.length } ?: return true

        val containingClassType = symbol.parent.type.resolve(function) as? OCObjectType

        if (!processRefs(ocQueryParameters, symbol, psiWrapper, containingClassType, methodSelector, searchWord, false, false, consumer)) {
            return false
        }

        val getterName = OCNameSuggester.getObjCGetterFromSetter(methodSelector) ?: return true

        if (getterName.isNotEmpty()) {
            if (!processRefs(ocQueryParameters, symbol, psiWrapper, containingClassType, getterName, getterName, true, false, consumer)) {
                return false
            }
        }

        return true
    }
}