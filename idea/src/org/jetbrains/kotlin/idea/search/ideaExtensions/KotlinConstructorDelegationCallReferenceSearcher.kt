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
import com.intellij.psi.PsiReference
import com.intellij.psi.search.searches.MethodReferencesSearch.SearchParameters
import com.intellij.util.Processor
import org.jetbrains.kotlin.idea.references.mainReference
import org.jetbrains.kotlin.idea.search.usagesSearch.processDelegationCallConstructorUsages

class KotlinConstructorDelegationCallReferenceSearcher() : QueryExecutorBase<PsiReference, SearchParameters>(true) {
    override fun processQuery(queryParameters: SearchParameters, consumer: Processor<PsiReference>) {
        val method = queryParameters.method
        if (!method.isConstructor) return

        method.processDelegationCallConstructorUsages(method.useScope.intersectWith(queryParameters.effectiveSearchScope)) {
            it.calleeExpression?.mainReference?.let { consumer.process(it) } ?: true
        }
    }
}