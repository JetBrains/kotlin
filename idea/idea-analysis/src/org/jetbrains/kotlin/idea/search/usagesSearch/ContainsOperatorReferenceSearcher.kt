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
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.utils.addToStdlib.firstIsInstance

class ContainsOperatorReferenceSearcher(
        targetFunction: PsiElement,
        searchScope: SearchScope,
        consumer: Processor<PsiReference>,
        optimizer: SearchRequestCollector
) : OperatorReferenceSearcher<KtOperationReferenceExpression>(targetFunction, searchScope, consumer, optimizer, wordsToSearch = listOf("in")) {

    private val OPERATION_TOKENS = setOf(KtTokens.IN_KEYWORD, KtTokens.NOT_IN)

    override fun processPossibleReceiverExpression(expression: KtExpression) {
        val parent = expression.parent
        when (parent) {
            is KtBinaryExpression -> {
                if (parent.operationToken in OPERATION_TOKENS && expression == parent.right) {
                    processReferenceElement(parent.operationReference)
                }
            }

            is KtWhenConditionInRange -> {
                processReferenceElement(parent.operationReference)
            }
        }
    }

    override fun isReferenceToCheck(ref: PsiReference): Boolean {
        if (ref !is KtSimpleNameReference) return false
        val element = ref.element as? KtOperationReferenceExpression ?: return false
        return element.getReferencedNameElementType() in OPERATION_TOKENS
    }

    override fun extractReference(element: KtElement): PsiReference? {
        val referenceExpression = element as? KtOperationReferenceExpression ?: return null
        if (referenceExpression.getReferencedNameElementType() !in OPERATION_TOKENS) return null
        return referenceExpression.references.firstIsInstance<KtSimpleNameReference>()
    }
}