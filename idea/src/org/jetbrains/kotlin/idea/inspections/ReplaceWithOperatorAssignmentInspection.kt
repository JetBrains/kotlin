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

import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.tree.IElementType
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.idea.analysis.analyzeAsReplacement
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.project.builtIns
import org.jetbrains.kotlin.idea.util.psi.patternMatching.matches
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.calls.callUtil.getType
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode

class ReplaceWithOperatorAssignmentInspection : AbstractApplicabilityBasedInspection<KtBinaryExpression>(
    KtBinaryExpression::class.java
) {

    override fun isApplicable(element: KtBinaryExpression): Boolean {
        if (element.operationToken != KtTokens.EQ) return false
        val left = element.left as? KtNameReferenceExpression ?: return false
        val right = element.right as? KtBinaryExpression ?: return false
        if (right.left == null || right.right == null) return false

        val bindingContext = right.analyze(BodyResolveMode.PARTIAL_WITH_CFA)
        if (!checkExpressionRepeat(left, right, bindingContext)) return false

        // now check that the resulting operator assignment will be resolved
        val opAssign = buildOperatorAssignment(element)
        opAssign.containingKtFile.doNotAnalyze = null //TODO: strange hack
        val newBindingContext = opAssign.analyzeAsReplacement(element, bindingContext)
        return newBindingContext.diagnostics.forElement(opAssign.operationReference).isEmpty()
    }

    override fun inspectionText(element: KtBinaryExpression) = "Replaceable with operator-assignment"

    override val defaultFixText = "Replace with operator-assignment"

    override fun fixText(element: KtBinaryExpression) =
        "Replace with '${(element.right as? KtBinaryExpression)?.operationReference?.operationSignTokenType?.value}='"

    override fun inspectionHighlightType(element: KtBinaryExpression): ProblemHighlightType {
        val left = element.left as? KtNameReferenceExpression
        if (left != null) {
            val context = left.analyze(BodyResolveMode.PARTIAL)
            val leftType = left.getType(context)
            if (leftType?.isReadOnlyCollectionOrMap(element.builtIns) == true) return ProblemHighlightType.INFORMATION
        }
        return ProblemHighlightType.GENERIC_ERROR_OR_WARNING
    }

    private fun checkExpressionRepeat(
        variableExpression: KtNameReferenceExpression,
        expression: KtBinaryExpression,
        bindingContext: BindingContext
    ): Boolean {
        val descriptor = bindingContext[BindingContext.REFERENCE_TARGET, expression.operationReference]?.containingDeclaration
        val isPrimitiveOperation = descriptor is ClassDescriptor && KotlinBuiltIns.isPrimitiveType(descriptor.defaultType)

        val operationToken = expression.operationToken
        val expressionLeft = expression.left
        val expressionRight = expression.right
        return when {
            variableExpression.matches(expressionLeft) -> {
                isArithmeticOperation(operationToken)
            }

            variableExpression.matches(expressionRight) -> {
                isPrimitiveOperation && isCommutative(operationToken)
            }

            expressionLeft is KtBinaryExpression -> {
                val sameCommutativeOperation = expressionLeft.operationToken == operationToken && isCommutative(operationToken)
                isPrimitiveOperation && sameCommutativeOperation && checkExpressionRepeat(
                    variableExpression,
                    expressionLeft,
                    bindingContext
                )
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

    override fun applyTo(element: PsiElement, project: Project, editor: Editor?) {
        (element as? KtBinaryExpression)?.replace(buildOperatorAssignment(element))
    }

    private fun buildOperatorAssignment(element: KtBinaryExpression): KtBinaryExpression {
        val replacement = buildOperatorAssignmentText(
            element.left as KtNameReferenceExpression,
            element.right as KtBinaryExpression,
            ""
        )
        return KtPsiFactory(element).createExpression(replacement) as KtBinaryExpression
    }

    private tailrec fun buildOperatorAssignmentText(
        variableExpression: KtNameReferenceExpression,
        expression: KtBinaryExpression,
        tail: String
    ): String {
        val operationText = expression.operationReference.text
        val variableName = variableExpression.text

        fun String.appendTail() = if (tail.isEmpty()) this else "$this $tail"

        return when {
            variableExpression.matches(expression.left) ->
                "$variableName $operationText= ${expression.right!!.text}".appendTail()

            variableExpression.matches(expression.right) ->
                "$variableName $operationText= ${expression.left!!.text}".appendTail()

            expression.left is KtBinaryExpression ->
                buildOperatorAssignmentText(
                    variableExpression,
                    expression.left as KtBinaryExpression,
                    "$operationText ${expression.right!!.text}".appendTail()
                )

            else ->
                tail
        }
    }
}
