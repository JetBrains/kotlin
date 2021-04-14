/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.quickfix

import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.idea.KotlinBundle
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.endOffset
import org.jetbrains.kotlin.psi.psiUtil.getParentOfType
import org.jetbrains.kotlin.psi.psiUtil.getStrictParentOfType

abstract class ReplaceCallFix(
    expression: KtQualifiedExpression,
    private val operation: String,
    private val notNullNeeded: Boolean = false
) : KotlinPsiOnlyQuickFixAction<KtQualifiedExpression>(expression) {

    override fun getFamilyName() = text

    override fun isAvailable(project: Project, editor: Editor?, file: KtFile): Boolean {
        val element = element ?: return false
        return element.selectorExpression != null
    }

    override fun invoke(project: Project, editor: Editor?, file: KtFile) {
        val element = element ?: return
        val elvis = element.elvisOrEmpty(notNullNeeded)
        val betweenReceiverAndOperation = element.elementsBetweenReceiverAndOperation().joinToString(separator = "") { it.text }
        val newExpression = KtPsiFactory(element).createExpressionByPattern(
            "$0$betweenReceiverAndOperation$operation$1$elvis",
            element.receiverExpression,
            element.selectorExpression!!
        )
        val replacement = element.replace(newExpression)
        if (elvis.isNotEmpty()) {
            replacement.moveCaretToEnd(editor, project)
        }
    }

    private fun KtQualifiedExpression.elementsBetweenReceiverAndOperation(): List<PsiElement> {
        val receiver = receiverExpression
        val operation = operationTokenNode as? PsiElement ?: return emptyList()
        val start = receiver.nextSibling?.takeIf { it != operation } ?: return emptyList()
        val end = operation.prevSibling?.takeIf { it != receiver } ?: return emptyList()
        return PsiTreeUtil.getElementsOfRange(start, end)
    }
}

class ReplaceImplicitReceiverCallFix(
    expression: KtExpression,
    private val notNullNeeded: Boolean
) : KotlinPsiOnlyQuickFixAction<KtExpression>(expression) {
    override fun getFamilyName() = text

    override fun getText() = KotlinBundle.message("replace.with.safe.this.call")

    override fun invoke(project: Project, editor: Editor?, file: KtFile) {
        val element = element ?: return
        val elvis = element.elvisOrEmpty(notNullNeeded)
        val newExpression = KtPsiFactory(element).createExpressionByPattern("this?.$0$elvis", element)
        val replacement = element.replace(newExpression)
        if (elvis.isNotEmpty()) {
            replacement.moveCaretToEnd(editor, project)
        }
    }
}

class ReplaceWithSafeCallFix(
    expression: KtDotQualifiedExpression,
    notNullNeeded: Boolean
) : ReplaceCallFix(expression, "?.", notNullNeeded) {
    override fun getText() = KotlinBundle.message("replace.with.safe.call")
}

class ReplaceWithSafeCallForScopeFunctionFix(
    expression: KtDotQualifiedExpression,
    notNullNeeded: Boolean
) : ReplaceCallFix(expression, "?.", notNullNeeded) {
    override fun getText() = KotlinBundle.message("replace.scope.function.with.safe.call")
}

class ReplaceWithDotCallFix(expression: KtSafeQualifiedExpression) : ReplaceCallFix(expression, "."), CleanupFix {
    override fun getText() = KotlinBundle.message("replace.with.dot.call")

    companion object : QuickFixesPsiBasedFactory<PsiElement>(PsiElement::class, PsiElementSuitabilityCheckers.ALWAYS_SUITABLE) {
        override fun doCreateQuickFix(psiElement: PsiElement): List<IntentionAction> {
            val qualifiedExpression = psiElement.getParentOfType<KtSafeQualifiedExpression>(strict = false) ?: return emptyList()
            return listOfNotNull(ReplaceWithDotCallFix(qualifiedExpression))
        }
    }
}

fun KtExpression.elvisOrEmpty(notNullNeeded: Boolean): String {
    if (!notNullNeeded) return ""
    val binaryExpression = getStrictParentOfType<KtBinaryExpression>()
    return if (binaryExpression?.left == this && binaryExpression.operationToken == KtTokens.ELVIS) "" else "?:"
}

fun PsiElement.moveCaretToEnd(editor: Editor?, project: Project) {
    editor?.run {
        PsiDocumentManager.getInstance(project).doPostponedOperationsAndUnblockDocument(document)
        val endOffset = if (text.endsWith(")")) endOffset - 1 else endOffset
        document.insertString(endOffset, " ")
        caretModel.moveToOffset(endOffset + 1)
    }
}
