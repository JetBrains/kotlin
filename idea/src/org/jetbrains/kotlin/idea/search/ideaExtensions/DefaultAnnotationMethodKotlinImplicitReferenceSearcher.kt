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
import com.intellij.openapi.application.ReadActionProcessor
import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiReference
import com.intellij.psi.search.searches.MethodReferencesSearch
import com.intellij.psi.search.searches.ReferencesSearch
import com.intellij.psi.util.PsiUtil
import org.jetbrains.kotlin.compatibility.ExecutorProcessor
import org.jetbrains.kotlin.idea.references.KtSimpleNameReference
import org.jetbrains.kotlin.idea.search.restrictToKotlinSources
import org.jetbrains.kotlin.idea.util.application.runReadAction
import org.jetbrains.kotlin.psi.KtAnnotationEntry
import org.jetbrains.kotlin.psi.KtValueArgument
import org.jetbrains.kotlin.psi.psiUtil.getParentOfTypeAndBranch
import org.jetbrains.kotlin.utils.addToStdlib.firstIsInstanceOrNull
import org.jetbrains.kotlin.idea.references.KotlinDefaultAnnotationMethodImplicitReferenceContributor.ReferenceImpl as ImplicitReference

class DefaultAnnotationMethodKotlinImplicitReferenceSearcher :
    QueryExecutorBase<PsiReference, MethodReferencesSearch.SearchParameters>(true) {
    private val PsiMethod.isDefaultAnnotationMethod: Boolean
        get() = PsiUtil.isAnnotationMethod(this) && name == PsiAnnotation.DEFAULT_REFERENCED_METHOD_NAME && parameterList.parametersCount == 0

    private fun createReferenceProcessor(consumer: ExecutorProcessor<PsiReference>) = object : ReadActionProcessor<PsiReference>() {
        override fun processInReadAction(reference: PsiReference): Boolean {
            if (reference !is KtSimpleNameReference) return true
            val annotationEntry = reference.expression.getParentOfTypeAndBranch<KtAnnotationEntry> { typeReference } ?: return true
            val argument = annotationEntry.valueArguments.singleOrNull() as? KtValueArgument ?: return true
            val implicitRef = argument.references.firstIsInstanceOrNull<ImplicitReference>() ?: return true
            return consumer.process(implicitRef)
        }
    }

    override fun processQuery(queryParameters: MethodReferencesSearch.SearchParameters, consumer: ExecutorProcessor<PsiReference>) {
        runReadAction {
            val method = queryParameters.method
            if (!method.isDefaultAnnotationMethod) return@runReadAction null
            val annotationClass = method.containingClass ?: return@runReadAction null
            val searchScope = queryParameters.effectiveSearchScope.restrictToKotlinSources()
            ReferencesSearch.search(annotationClass, searchScope)
        }?.forEach(createReferenceProcessor(consumer))
    }
}
