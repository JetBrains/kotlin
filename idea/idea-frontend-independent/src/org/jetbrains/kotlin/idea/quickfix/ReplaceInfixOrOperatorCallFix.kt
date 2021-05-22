/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.quickfix

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.idea.KotlinBundle
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getAssignmentByLHS
import org.jetbrains.kotlin.types.expressions.OperatorConventions

class ReplaceInfixOrOperatorCallFix(
    element: KtExpression,
    private val notNullNeeded: Boolean,
    private val binaryOperatorName: String = ""
) : KotlinPsiOnlyQuickFixAction<KtExpression>(element) {

    override fun getText() = KotlinBundle.message("replace.with.safe.call")

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
                val left = element.left ?: return
                val right = element.right ?: return

                // `a += b` is replaced with `a = a?.plus(b)` when the += operator is not available
                val isNormalAssignment = element.operationToken in KtTokens.AUGMENTED_ASSIGNMENTS &&
                        Name.identifier(binaryOperatorName) !in OperatorConventions.ASSIGNMENT_OPERATIONS.values
                val newExpression = if (isNormalAssignment) {
                    psiFactory.createExpressionByPattern("$0 = $0?.$1($2)", left, binaryOperatorName, right)
                } else {
                    psiFactory.createExpressionByPattern("$0?.$1($2)$elvis", left, binaryOperatorName, right)
                }
                replacement = element.replace(newExpression)
            }
        }
        if (elvis.isNotEmpty()) {
            replacement?.moveCaretToEnd(editor, project)
        }
    }
}
