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
import com.intellij.util.Processor
import org.jetbrains.kotlin.asJava.namedUnwrappedElement
import org.jetbrains.kotlin.idea.caches.resolve.util.getJavaMethodDescriptor
import org.jetbrains.kotlin.idea.search.restrictToKotlinSources
import org.jetbrains.kotlin.incremental.components.LookupTracker
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.storage.LockBasedStorageManager
import org.jetbrains.kotlin.synthetic.JavaSyntheticPropertiesScope
import org.jetbrains.kotlin.synthetic.SyntheticJavaPropertyDescriptor
import org.jetbrains.kotlin.synthetic.canBePropertyAccessor

class KotlinPropertyAccessorsReferenceSearcher : QueryExecutorBase<PsiReference, MethodReferencesSearch.SearchParameters>(true) {
    override fun processQuery(queryParameters: MethodReferencesSearch.SearchParameters, consumer: Processor<PsiReference>) {
        val method = queryParameters.method
        val onlyKotlinFiles = queryParameters.effectiveSearchScope.restrictToKotlinSources()
        if (onlyKotlinFiles == GlobalSearchScope.EMPTY_SCOPE) return

        val propertyName = propertyName(method) ?: return

        queryParameters.optimizer!!.searchWord(
                propertyName,
                onlyKotlinFiles,
                UsageSearchContext.IN_CODE,
                true,
                method)
    }

    private fun propertyName(method: PsiMethod): String? {
        val unwrapped = method.namedUnwrappedElement
        if (unwrapped is KtProperty) {
            return unwrapped.getName()
        }

        if (!canBePropertyAccessor(method.name)) return null
        val functionDescriptor = method.getJavaMethodDescriptor() ?: return null
        val syntheticExtensionsScope = JavaSyntheticPropertiesScope(LockBasedStorageManager(), LookupTracker.DO_NOTHING)
        val property = SyntheticJavaPropertyDescriptor.findByGetterOrSetter(functionDescriptor, syntheticExtensionsScope) ?: return null
        return property.name.asString()
    }
}
