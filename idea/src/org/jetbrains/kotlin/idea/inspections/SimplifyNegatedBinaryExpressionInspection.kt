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

package org.jetbrains.kotlin.idea.inspections

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.tree.IElementType
import org.jetbrains.kotlin.lexer.KtSingleValueToken
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.*

class SimplifyNegatedBinaryExpressionInspection : AbstractApplicabilityBasedInspection<KtPrefixExpression>(
        KtPrefixExpression::class.java
) {

    private fun IElementType.negate(): KtSingleValueToken? = when (this) {
        KtTokens.IN_KEYWORD -> KtTokens.NOT_IN
        KtTokens.NOT_IN -> KtTokens.IN_KEYWORD

        KtTokens.IS_KEYWORD -> KtTokens.NOT_IS
        KtTokens.NOT_IS -> KtTokens.IS_KEYWORD

        KtTokens.EQEQ -> KtTokens.EXCLEQ
        KtTokens.EXCLEQ -> KtTokens.EQEQ

        KtTokens.LT -> KtTokens.GTEQ
        KtTokens.GTEQ -> KtTokens.LT

        KtTokens.GT -> KtTokens.LTEQ
        KtTokens.LTEQ -> KtTokens.GT

        else -> null
    }

    override fun inspectionTarget(element: KtPrefixExpression) = element.operationReference

    override fun inspectionText(element: KtPrefixExpression) = "Negated operation should be simplified"

    override val defaultFixText = "Simplify negated operation"

    override fun fixText(element: KtPrefixExpression): String {
        val expression = KtPsiUtil.deparenthesize(element.baseExpression) as? KtOperationExpression ?: return defaultFixText
        val operation = expression.operationReference.getReferencedNameElementType() as? KtSingleValueToken ?: return defaultFixText
        val negatedOperation = operation.negate() ?: return defaultFixText
        return "Replace negated '${operation.value}' operation with '${negatedOperation.value}'"
    }

    override fun isApplicable(element: KtPrefixExpression): Boolean {
        if (element.operationToken != KtTokens.EXCL) return false

        val expression = KtPsiUtil.deparenthesize(element.baseExpression) as? KtOperationExpression ?: return false
        when (expression) {
            is KtIsExpression -> if (expression.typeReference == null) return false
            is KtBinaryExpression -> if (expression.left == null || expression.right == null) return false
            else -> return false
        }

        return (expression.operationReference.getReferencedNameElementType() as? KtSingleValueToken)?.negate() != null
    }

    override fun applyTo(element: PsiElement, project: Project, editor: Editor?) {
        val prefixExpression = element.parent as? KtPrefixExpression ?: return
        val expression = KtPsiUtil.deparenthesize(prefixExpression.baseExpression) ?: return
        val operation = (expression as KtOperationExpression).operationReference.getReferencedNameElementType().negate()?.value ?: return

        val psiFactory = KtPsiFactory(expression)
        val newExpression = when (expression) {
            is KtIsExpression ->
                psiFactory.createExpressionByPattern("$0 $1 $2", expression.leftHandSide, operation, expression.typeReference!!)
            is KtBinaryExpression ->
                psiFactory.createExpressionByPattern("$0 $1 $2", expression.left!!, operation, expression.right!!)
            else ->
                throw IllegalArgumentException()
        }
        prefixExpression.replace(newExpression)
    }
}