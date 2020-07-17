/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.intentions

import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiComment
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiWhiteSpace
import org.jetbrains.kotlin.idea.KotlinBundle
import org.jetbrains.kotlin.idea.refactoring.getLineNumber
import org.jetbrains.kotlin.idea.util.CommentSaver
import org.jetbrains.kotlin.j2k.isInSingleLine
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.*

class AddBracesIntention : SelfTargetingIntention<KtElement>(KtElement::class.java, KotlinBundle.lazyMessage("add.braces")) {
    override fun isApplicableTo(element: KtElement, caretOffset: Int): Boolean {
        val expression = element.getTargetExpression(caretOffset) ?: return false
        if (expression is KtBlockExpression) return false

        return when (val parent = expression.parent) {
            is KtContainerNode -> {
                val description = parent.description() ?: return false
                setTextGetter(KotlinBundle.lazyMessage("add.braces.to.0.statement", description))
                true
            }
            is KtWhenEntry -> {
                setTextGetter(KotlinBundle.lazyMessage("add.braces.to.when.entry"))
                true
            }
            else -> {
                false
            }
        }
    }

    override fun applyTo(element: KtElement, editor: Editor?) {
        if (editor == null) throw IllegalArgumentException("This intention requires an editor")
        val expression = element.getTargetExpression(editor.caretModel.offset) ?: return
        var isCommentBeneath = false
        var isCommentInside = false
        val psiFactory = KtPsiFactory(element)

        val semicolon = element.getNextSiblingIgnoringWhitespaceAndComments()?.takeIf { it.node.elementType == KtTokens.SEMICOLON }
        if (semicolon != null) {
            val afterSemicolon = semicolon.getNextSiblingIgnoringWhitespace()
            if (semicolon.getLineNumber() == afterSemicolon?.getLineNumber())
                semicolon.replace(psiFactory.createNewLine())
            else
                semicolon.delete()
        }

        if (element is KtIfExpression) {
            // Check if a comment is actually underneath (\n) the expression
            val allElements = element.siblings(withItself = false).filterIsInstance<PsiElement>()
            val sibling = allElements.firstOrNull { it is PsiComment }
            if (sibling is PsiComment) {
                // Check if \n before first received comment sibling
                // if false, the normal procedure of adding braces occurs.
                isCommentBeneath =
                    sibling.prevSibling is PsiWhiteSpace &&
                            sibling.prevSibling.textContains('\n') &&
                            (sibling.prevSibling.prevSibling is PsiComment || sibling.prevSibling.prevSibling is PsiElement)
            }
        }

        // Check for nested if/else
        if (element is KtIfExpression && expression.isInSingleLine() && element.`else` != null &&
            element.parent.nextSibling is PsiWhiteSpace &&
            element.parent.nextSibling.nextSibling is PsiComment
        ) {
            isCommentInside = true
        }

        val nextComment = when {
            element is KtDoWhileExpression -> null // bound to the closing while
            element is KtIfExpression && expression === element.then && element.`else` != null -> null // bound to else
            else -> element.getNextSiblingIgnoringWhitespace().takeIf {
                it is PsiComment && it.getLineNumber() == element.getLineNumber(start = false)
            }
        }

        val saver = when {
            isCommentInside -> {
                CommentSaver(element.parent.nextSibling.nextSibling)
            }
            else -> if (nextComment == null) CommentSaver(element) else CommentSaver(PsiChildRange(element, nextComment))
        }

        if (isCommentInside) {
            element.parent.nextSibling.nextSibling.delete()
        }

        element.allChildren.filterIsInstance<PsiComment>().toList().forEach {
            it.delete()
        }
        nextComment?.delete()

        val result = expression.replace(psiFactory.createSingleStatementBlock(expression))

        when (element) {
            is KtDoWhileExpression ->
                // remove new line between '}' and while
                (element.body?.parent?.nextSibling as? PsiWhiteSpace)?.delete()
            is KtIfExpression ->
                (result?.parent?.nextSibling as? PsiWhiteSpace)?.delete()
        }

        // Check for single line expression with comment beneath.
        saver.restore(result, isCommentBeneath, isCommentInside, forceAdjustIndent = false)
    }

    private fun KtElement.getTargetExpression(caretLocation: Int): KtExpression? {
        return when (this) {
            is KtIfExpression -> {
                val thenExpr = then ?: return null
                val elseExpr = `else`
                if (elseExpr != null && caretLocation >= elseKeyword?.startOffset ?: return null) {
                    elseExpr
                } else {
                    thenExpr
                }
            }

            is KtLoopExpression -> body
            is KtWhenEntry -> expression
            else -> null
        }
    }
}
