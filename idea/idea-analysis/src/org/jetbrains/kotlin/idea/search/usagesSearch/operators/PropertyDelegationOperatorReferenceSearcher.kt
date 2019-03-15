/*
 * Copyright 2010-2017 JetBrains s.r.o.
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

package org.jetbrains.kotlin.idea.search.usagesSearch.operators

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReference
import com.intellij.psi.search.SearchRequestCollector
import com.intellij.psi.search.SearchScope
import org.jetbrains.kotlin.compatibility.ExecutorProcessor
import org.jetbrains.kotlin.idea.references.KtPropertyDelegationMethodsReference
import org.jetbrains.kotlin.idea.search.ideaExtensions.KotlinReferencesSearchOptions
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtPropertyDelegate
import org.jetbrains.kotlin.utils.addToStdlib.firstIsInstance

class PropertyDelegationOperatorReferenceSearcher(
    targetFunction: PsiElement,
    searchScope: SearchScope,
    consumer: ExecutorProcessor<PsiReference>,
    optimizer: SearchRequestCollector,
    options: KotlinReferencesSearchOptions
) : OperatorReferenceSearcher<KtPropertyDelegate>(targetFunction, searchScope, consumer, optimizer, options, wordsToSearch = listOf("by")) {

    override fun processPossibleReceiverExpression(expression: KtExpression) {
        (expression.parent as? KtPropertyDelegate)?.let { processReferenceElement(it) }
    }

    override fun isReferenceToCheck(ref: PsiReference): Boolean {
        return ref is KtPropertyDelegationMethodsReference
    }

    override fun extractReference(element: KtElement): PsiReference? {
        return (element as? KtPropertyDelegate)?.references?.firstIsInstance<KtPropertyDelegationMethodsReference>()
    }
}