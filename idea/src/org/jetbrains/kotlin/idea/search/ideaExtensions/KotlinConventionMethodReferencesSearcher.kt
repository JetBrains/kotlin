/*
 * Copyright 2010-2016 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.idea.search.ideaExtensions

import com.intellij.openapi.application.QueryExecutorBase
import com.intellij.psi.PsiReference
import com.intellij.psi.search.UsageSearchContext
import com.intellij.psi.search.searches.MethodReferencesSearch
import com.intellij.util.Processor
import org.jetbrains.kotlin.idea.search.restrictToKotlinSources
import org.jetbrains.kotlin.idea.search.usagesSearch.OperatorReferenceSearcher
import org.jetbrains.kotlin.idea.search.usagesSearch.getOperationSymbolsToSearch
import org.jetbrains.kotlin.idea.util.application.runReadAction
import org.jetbrains.kotlin.lexer.KtSingleValueToken
import org.jetbrains.kotlin.name.Name

class KotlinConventionMethodReferencesSearcher() : QueryExecutorBase<PsiReference, MethodReferencesSearch.SearchParameters>(true) {
    override fun processQuery(queryParameters: MethodReferencesSearch.SearchParameters, consumer: Processor<PsiReference>) {
        val method = queryParameters.method
        val name = runReadAction { method.name }
        if (!Name.isValidIdentifier(name)) return
        val identifier = Name.identifier(name)

        val operatorSearcher = OperatorReferenceSearcher.create(
                method, queryParameters.effectiveSearchScope, consumer, queryParameters.optimizer, KotlinReferencesSearchOptions(acceptCallableOverrides = true))
        if (operatorSearcher != null) {
            operatorSearcher.run()
        }
        else {
            val operationSymbolsToSearch = identifier.getOperationSymbolsToSearch() ?: return
            val wordsToSearch = operationSymbolsToSearch.first.map { (it as KtSingleValueToken).value }
            val resultProcessor = KotlinRequestResultProcessor(method,
                                                               filter = { ref -> ref.javaClass == operationSymbolsToSearch.second })

            wordsToSearch.forEach { word ->
                queryParameters.optimizer.searchWord(word, queryParameters.effectiveSearchScope.restrictToKotlinSources(),
                                                     UsageSearchContext.IN_CODE, true, method,
                                                     resultProcessor)
            }
        }
    }
}
