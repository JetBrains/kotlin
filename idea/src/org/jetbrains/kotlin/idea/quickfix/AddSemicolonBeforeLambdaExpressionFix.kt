/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.quickfix

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.KtNodeTypes
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.idea.quickfix.quickfixUtil.createIntentionForFirstParentOfType
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.startOffset
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

class AddSemicolonBeforeLambdaExpressionFix(element: KtLambdaExpression) : KotlinQuickFixAction<KtLambdaExpression>(element) {
    override fun getText(): String = "Terminate preceding call with semicolon"
    override fun getFamilyName(): String = text

    override fun invoke(project: Project, editor: Editor?, file: KtFile) {
        val lambdaExpressionArgument = element?.parent?.safeAs<KtLambdaArgument>()
            ?: return
        val callExpression = lambdaExpressionArgument.parent.safeAs<KtCallExpression>()
            ?: return
        val desiredEndOfCallExpression =
            PsiTreeUtil.findSiblingBackward(
                lambdaExpressionArgument,
                KtNodeTypes.LAMBDA_ARGUMENT,
                null
            ) ?: PsiTreeUtil.findSiblingBackward(
                lambdaExpressionArgument,
                KtNodeTypes.VALUE_ARGUMENT_LIST,
                null
            )
        desiredEndOfCallExpression?.let { endOfCall ->
            makeNewExpressionsFromFollowingLambdas(callExpression, endOfCall)
            val semicolon = callExpression.parent.addAfter(
                KtPsiFactory(project).createSemicolon(),
                callExpression
            )
            editor?.caretModel?.moveToOffset(semicolon.startOffset)
        }
    }

    private fun makeNewExpressionsFromFollowingLambdas(
        oldCallExpression: KtCallExpression,
        endOfArguments: PsiElement
    ) {
        var lastSibling = oldCallExpression.lastChild
        val parentForCallExpression = oldCallExpression.parent

        while (lastSibling != endOfArguments) {
            when (lastSibling) {
                is KtLambdaArgument -> parentForCallExpression.addAfter(
                    lastSibling.getLambdaExpression() ?: lastSibling,
                    oldCallExpression
                )
                else -> parentForCallExpression.addAfter(
                    lastSibling,
                    oldCallExpression
                )
            }
            lastSibling = lastSibling.prevSibling
        }

        oldCallExpression.deleteChildRange(endOfArguments.nextSibling, oldCallExpression.lastChild)
    }

    companion object Factory : KotlinSingleIntentionActionFactory() {
        override fun createAction(diagnostic: Diagnostic) =
            diagnostic.createIntentionForFirstParentOfType(::AddSemicolonBeforeLambdaExpressionFix)
    }
}