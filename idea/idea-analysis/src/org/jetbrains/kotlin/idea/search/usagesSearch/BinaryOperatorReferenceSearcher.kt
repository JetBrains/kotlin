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
import org.jetbrains.kotlin.idea.references.KtSimpleNameReference
import org.jetbrains.kotlin.lexer.KtSingleValueToken
import org.jetbrains.kotlin.psi.KtBinaryExpression
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.utils.addToStdlib.firstIsInstance

class BinaryOperatorReferenceSearcher(
        targetFunction: PsiElement,
        private val operationTokens: List<KtSingleValueToken>,
        searchScope: SearchScope,
        consumer: Processor<PsiReference>,
        optimizer: SearchRequestCollector
) : OperatorReferenceSearcher<KtBinaryExpression>(targetFunction, searchScope, consumer, optimizer, wordsToSearch = operationTokens.map { it.value }) {

    override fun processPossibleReceiverExpression(expression: KtExpression) {
        val binaryExpression = expression.parent as? KtBinaryExpression ?: return
        if (binaryExpression.operationToken !in operationTokens) return
        if (expression != binaryExpression.left) return
        processReferenceElement(binaryExpression)
    }

    override fun isReferenceToCheck(ref: PsiReference): Boolean {
        if (ref !is KtSimpleNameReference) return false
        val element = ref.element
        if (element.parent !is KtBinaryExpression) return false
        return element.getReferencedNameElementType() in operationTokens
    }

    override fun extractReference(element: PsiElement): PsiReference? {
        val binaryExpression = element as? KtBinaryExpression ?: return null
        if (binaryExpression.operationToken !in operationTokens) return null
        return binaryExpression.operationReference.references.firstIsInstance<KtSimpleNameReference>()
    }
}