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
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtFunction
import org.jetbrains.kotlin.psi.KtUnaryExpression
import org.jetbrains.kotlin.utils.addToStdlib.firstIsInstance

class UnaryOperatorReferenceSearcher(
        targetFunction: PsiElement,
        private val operationToken: KtSingleValueToken,
        searchScope: SearchScope,
        consumer: Processor<PsiReference>,
        optimizer: SearchRequestCollector
) : OperatorReferenceSearcher<KtUnaryExpression>(targetFunction, searchScope, consumer, optimizer, wordsToSearch = listOf(operationToken.value)) {

    override fun processPossibleReceiverExpression(expression: KtExpression) {
        val unaryExpression = expression.parent as? KtUnaryExpression ?: return
        if (unaryExpression.operationToken != operationToken) return
        processReferenceElement(unaryExpression)
    }

    override fun isReferenceToCheck(ref: PsiReference): Boolean {
        if (ref !is KtSimpleNameReference) return false
        val element = ref.element
        if (element.parent !is KtUnaryExpression) return false
        return element.getReferencedNameElementType() == operationToken
    }

    override fun extractReference(element: KtElement): PsiReference? {
        val unaryExpression = element as? KtUnaryExpression ?: return null
        if (unaryExpression.operationToken != operationToken) return null
        return unaryExpression.operationReference.references.firstIsInstance<KtSimpleNameReference>()
    }
}