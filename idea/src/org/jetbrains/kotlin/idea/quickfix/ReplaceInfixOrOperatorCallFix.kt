/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.quickfix

import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.idea.KotlinBundle
import org.jetbrains.kotlin.idea.caches.resolve.resolveToCall
import org.jetbrains.kotlin.idea.intentions.OperatorToFunctionIntention
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getAssignmentByLHS
import org.jetbrains.kotlin.resolve.calls.resolvedCallUtil.getImplicitReceiverValue
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode
import org.jetbrains.kotlin.types.expressions.OperatorConventions

class ReplaceInfixOrOperatorCallFix(
    element: KtExpression,
    private val notNullNeeded: Boolean
) : KotlinQuickFixAction<KtExpression>(element) {

    override fun getText() = KotlinBundle.message("replace.with.safe.call1")

    override fun getFamilyName() = text

    override fun invoke(project: Project, editor: Editor?, file: KtFile) {
        val element = element ?: return
        val psiFactory = KtPsiFactory(file)
        val elvis = element.elvisOrEmpty(notNullNeeded)
        var replacement: PsiElement? = null
        when (element) {
            is KtArrayAccessExpression -> {
                val assignment = element.getAssignmentByLHS()
                val right = assignment?.right
                val arrayExpression = element.arrayExpression ?: return
                if (assignment != null) {
                    if (right == null) return
                    val newExpression = psiFactory.createExpressionByPattern(
                        "$0?.set($1, $2)", arrayExpression, element.indexExpressions.joinToString(", ") { it.text }, right
                    )
                    assignment.replace(newExpression)
                } else {
                    val newExpression = psiFactory.createExpressionByPattern(
                        "$0?.get($1)$elvis", arrayExpression, element.indexExpressions.joinToString(", ") { it.text })
                    replacement = element.replace(newExpression)
                }
            }
            is KtCallExpression -> {
                val newExpression = psiFactory.createExpressionByPattern(
                    "$0?.invoke($1)$elvis", element.calleeExpression ?: return, element.valueArguments.joinToString(", ") { it.text })
                replacement = element.replace(newExpression)
            }
            is KtBinaryExpression -> {
                replacement = if (element.operationToken == KtTokens.IDENTIFIER) {
                    val newExpression = psiFactory.createExpressionByPattern(
                        "$0?.$1($2)$elvis", element.left ?: return, element.operationReference.text, element.right ?: return
                    )
                    element.replace(newExpression)
                } else {
                    val nameExpression = OperatorToFunctionIntention.convert(element).second
                    val callExpression = nameExpression.parent as KtCallExpression
                    val qualifiedExpression = callExpression.parent as KtDotQualifiedExpression
                    val safeExpression = psiFactory.createExpressionByPattern(
                        "$0?.$1$elvis", qualifiedExpression.receiverExpression, callExpression
                    )
                    qualifiedExpression.replace(safeExpression)
                }
            }
        }
        if (elvis.isNotEmpty()) {
            replacement?.moveCaretToEnd(editor, project)
        }
    }

    override fun startInWriteAction() = true

    companion object : KotlinSingleIntentionActionFactory() {
        override fun createAction(diagnostic: Diagnostic): IntentionAction? {
            val expression = diagnostic.psiElement
            if (expression is KtArrayAccessExpression && diagnostic.factory != Errors.UNSAFE_IMPLICIT_INVOKE_CALL) {
                if (expression.arrayExpression == null) return null
                return ReplaceInfixOrOperatorCallFix(expression, expression.shouldHaveNotNullType())
            }

            return when (val parent = expression.parent) {
                is KtBinaryExpression -> {
                    when {
                        parent.left == null || parent.right == null -> null
                        parent.operationToken == KtTokens.EQ -> null
                        parent.operationToken in OperatorConventions.COMPARISON_OPERATIONS -> null
                        else -> ReplaceInfixOrOperatorCallFix(parent, parent.shouldHaveNotNullType())
                    }
                }
                is KtCallExpression -> {
                    when {
                        parent.calleeExpression == null -> null
                        parent.parent is KtQualifiedExpression -> null
                        parent.resolveToCall(BodyResolveMode.FULL)?.getImplicitReceiverValue() != null -> null
                        else -> ReplaceInfixOrOperatorCallFix(parent, parent.shouldHaveNotNullType())
                    }
                }
                else -> null
            }
        }
    }
}
