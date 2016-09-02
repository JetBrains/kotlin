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

package org.jetbrains.kotlin.idea.search.usagesSearch

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReference
import com.intellij.psi.search.SearchRequestCollector
import com.intellij.psi.search.SearchScope
import com.intellij.util.Processor
import org.jetbrains.kotlin.idea.references.KtArrayAccessReference
import org.jetbrains.kotlin.psi.KtArrayAccessExpression
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtFunction
import org.jetbrains.kotlin.utils.addToStdlib.firstIsInstance

//TODO: more effective search of 'set'
class IndexingOperatorReferenceSearcher(
        targetFunction: KtFunction,
        searchScope: SearchScope,
        consumer: Processor<PsiReference>,
        optimizer: SearchRequestCollector
) : OperatorReferenceSearcher<KtArrayAccessExpression>(targetFunction, searchScope, consumer, optimizer, wordsToSearch = listOf("[")) {

    override fun processSuspiciousExpression(expression: KtExpression) {
        val accessExpression = expression.parent as? KtArrayAccessExpression ?: return
        if (expression != accessExpression.arrayExpression) return
        processReferenceElement(accessExpression)
    }

    override fun isReferenceToCheck(ref: PsiReference) = ref is KtArrayAccessReference

    override fun extractReference(element: PsiElement): PsiReference? {
        val accessExpression = element as? KtArrayAccessExpression ?: return null
        return accessExpression.references.firstIsInstance<KtArrayAccessReference>()
    }
}