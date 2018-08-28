/*
 * Copyright 2010-2015 JetBrains s.r.o.
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
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiReference
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.UsageSearchContext
import com.intellij.psi.search.searches.MethodReferencesSearch
import org.jetbrains.kotlin.asJava.namedUnwrappedElement
import org.jetbrains.kotlin.compatibility.ExecutorProcessor
import org.jetbrains.kotlin.idea.search.restrictToKotlinSources
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.synthetic.SyntheticJavaPropertyDescriptor

class KotlinPropertyAccessorsReferenceSearcher : QueryExecutorBase<PsiReference, MethodReferencesSearch.SearchParameters>(true) {
    override fun processQuery(queryParameters: MethodReferencesSearch.SearchParameters, consumer: ExecutorProcessor<PsiReference>) {
        val method = queryParameters.method
        val onlyKotlinFiles = queryParameters.effectiveSearchScope.restrictToKotlinSources()
        if (onlyKotlinFiles == GlobalSearchScope.EMPTY_SCOPE) return

        for (propertyName in propertyNames(method)) {
            queryParameters.optimizer!!.searchWord(
                propertyName,
                onlyKotlinFiles,
                UsageSearchContext.IN_CODE,
                true,
                method
            )
        }
    }

    private fun propertyNames(method: PsiMethod): List<String> {
        val unwrapped = method.namedUnwrappedElement
        if (unwrapped is KtProperty) {
            return listOfNotNull(unwrapped.getName())
        }

        return SyntheticJavaPropertyDescriptor.propertyNamesByAccessorName(Name.identifier(method.name)).map(Name::asString)
    }
}
