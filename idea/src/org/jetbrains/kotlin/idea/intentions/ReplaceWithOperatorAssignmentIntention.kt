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

package org.jetbrains.kotlin.idea.intentions

import com.intellij.openapi.editor.Editor
import com.intellij.psi.tree.IElementType
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.inspections.IntentionBasedInspection
import org.jetbrains.kotlin.idea.util.psi.patternMatching.matches
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtBinaryExpression
import org.jetbrains.kotlin.psi.KtNameReferenceExpression
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.resolve.BindingContext

public class ReplaceWithOperatorAssignmentInspection : IntentionBasedInspection<KtBinaryExpression>(ReplaceWithOperatorAssignmentIntention())

public class ReplaceWithOperatorAssignmentIntention : JetSelfTargetingOffsetIndependentIntention<KtBinaryExpression>(javaClass(), "Replace with operator-assignment") {

    override fun isApplicableTo(element: KtBinaryExpression): Boolean {
        if (element.getOperationToken() != KtTokens.EQ) return false
        val left = element.getLeft() as? KtNameReferenceExpression ?: return false
        val right = element.getRight() as? KtBinaryExpression ?: return false
        if (right.getLeft() == null || right.getRight() == null) return false

        return checkExpressionRepeat(left, right)
    }

    private fun checkExpressionRepeat(variableExpression: KtNameReferenceExpression, expression: KtBinaryExpression): Boolean {
        val context = expression.analyze()
        val descriptor = context[BindingContext.REFERENCE_TARGET, expression.getOperationReference()]?.getContainingDeclaration()
        val isPrimitiveOperation = descriptor is ClassDescriptor && KotlinBuiltIns.isPrimitiveType(descriptor.getDefaultType())

        val operationToken = expression.getOperationToken()
        setText("Replace with ${expression.getOperationReference().getText()}= expression")

        val expressionLeft = expression.getLeft()
        val expressionRight = expression.getRight()
        return when {
            variableExpression.matches(expressionLeft) -> {
                isArithmeticOperation(operationToken)
            }

            variableExpression.matches(expressionRight) -> {
                isPrimitiveOperation && isCommutative(operationToken)
            }

            expressionLeft is KtBinaryExpression -> {
                val sameCommutativeOperation = expressionLeft.getOperationToken() == operationToken && isCommutative(operationToken)
                isPrimitiveOperation && sameCommutativeOperation && checkExpressionRepeat(variableExpression, expressionLeft)
            }

            else -> {
                false
            }
        }
    }

    private fun isCommutative(operationToken: IElementType) = operationToken == KtTokens.PLUS || operationToken == KtTokens.MUL
    private fun isArithmeticOperation(operationToken: IElementType) = operationToken == KtTokens.PLUS ||
                                                                       operationToken == KtTokens.MINUS ||
                                                                       operationToken == KtTokens.MUL ||
                                                                       operationToken == KtTokens.DIV ||
                                                                       operationToken == KtTokens.PERC

    override fun applyTo(element: KtBinaryExpression, editor: Editor) {
        val replacement = buildOperatorAssignmentText(
                element.getLeft() as KtNameReferenceExpression,
                element.getRight() as KtBinaryExpression,
                ""
        )
        element.replace(KtPsiFactory(element).createExpression(replacement))
    }

    tailrec private fun buildOperatorAssignmentText(variableExpression: KtNameReferenceExpression, expression: KtBinaryExpression, tail: String): String {
        val operationText = expression.getOperationReference().getText()
        val variableName = variableExpression.getText()

        fun String.appendTail() = if (tail.isEmpty()) this else "$this $tail"

        return when {
            variableExpression.matches(expression.getLeft()) -> "$variableName $operationText= ${expression.getRight()!!.getText()}".appendTail()

            variableExpression.matches(expression.getRight()) -> "$variableName $operationText= ${expression.getLeft()!!.getText()}".appendTail()

            expression.getLeft() is KtBinaryExpression ->
                buildOperatorAssignmentText(variableExpression, expression.getLeft() as KtBinaryExpression, "$operationText ${expression.getRight()!!.getText()}".appendTail())

            else -> tail
        }
    }
}
