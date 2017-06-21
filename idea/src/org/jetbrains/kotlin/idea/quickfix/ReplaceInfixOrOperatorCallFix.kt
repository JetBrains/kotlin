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

package org.jetbrains.kotlin.idea.quickfix

import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.intentions.OperatorToFunctionIntention
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getAssignmentByLHS
import org.jetbrains.kotlin.resolve.calls.callUtil.getResolvedCall
import org.jetbrains.kotlin.resolve.calls.resolvedCallUtil.getImplicitReceiverValue
import org.jetbrains.kotlin.types.expressions.OperatorConventions

class ReplaceInfixOrOperatorCallFix(element: KtExpression) : KotlinQuickFixAction<KtExpression>(element) {

    override fun getText() = "Replace with safe (?.) call"

    override fun getFamilyName() = text

    override fun invoke(project: Project, editor: Editor?, file: KtFile) {
        val element = element ?: return
        val psiFactory = KtPsiFactory(file)
        when (element) {
            is KtArrayAccessExpression -> {
                val assignment = element.getAssignmentByLHS()
                val right = assignment?.right
                val arrayExpression = element.arrayExpression ?: return
                if (assignment != null) {
                    if (right == null) return
                    val newExpression = psiFactory.createExpressionByPattern(
                            "$0?.set($1, $2)", arrayExpression, element.indexExpressions.joinToString(", ") { it.text }, right)
                    assignment.replace(newExpression)
                }
                else {
                    val newExpression = psiFactory.createExpressionByPattern(
                            "$0?.get($1)", arrayExpression, element.indexExpressions.joinToString(", ") { it.text })
                    element.replace(newExpression)
                }
            }
            is KtCallExpression -> {
                val newExpression = psiFactory.createExpressionByPattern(
                        "$0?.invoke($1)", element.calleeExpression ?: return, element.valueArguments.joinToString(", ") { it.text })
                element.replace(newExpression)
            }
            is KtBinaryExpression -> {
                if (element.operationToken == KtTokens.IDENTIFIER) {
                    val newExpression = psiFactory.createExpressionByPattern(
                            "$0?.$1($2)", element.left ?: return, element.operationReference, element.right ?: return)
                    element.replace(newExpression)
                }
                else {
                    val nameExpression = OperatorToFunctionIntention.convert(element).second
                    val callExpression = nameExpression.parent as KtCallExpression
                    val qualifiedExpression = callExpression.parent as KtDotQualifiedExpression
                    val safeExpression = psiFactory.createExpressionByPattern(
                            "$0?.$1", qualifiedExpression.receiverExpression, callExpression)
                    qualifiedExpression.replace(safeExpression)
                }
            }
        }
    }

    override fun startInWriteAction() = true

    companion object : KotlinSingleIntentionActionFactory() {
        override fun createAction(diagnostic: Diagnostic): IntentionAction? {
            val expression = diagnostic.psiElement
            if (expression is KtArrayAccessExpression && diagnostic.factory != Errors.UNSAFE_IMPLICIT_INVOKE_CALL) {
                if (expression.arrayExpression == null) return null
                return ReplaceInfixOrOperatorCallFix(expression)
            }
            val parent = expression.parent
            return when (parent) {
                is KtBinaryExpression -> {
                    if (parent.left == null || parent.right == null) null
                    else {
                        if (parent.operationToken in OperatorConventions.COMPARISON_OPERATIONS) null
                        else ReplaceInfixOrOperatorCallFix(parent)
                    }
                }
                is KtCallExpression -> {
                    if (parent.calleeExpression == null) null
                    else if (parent.parent is KtQualifiedExpression) null
                    else if (parent.getResolvedCall(parent.analyze())?.getImplicitReceiverValue() != null) null
                    else ReplaceInfixOrOperatorCallFix(parent)
                }
                else -> null
            }
        }
    }
}
