/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.intentions

import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiComment
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiWhiteSpace
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtPsiFactory

class ConvertBlockCommentToLineCommentIntention : SelfTargetingIntention<PsiComment>(
    PsiComment::class.java, "Replace with end of line comment"
) {
    override fun isApplicableTo(element: PsiComment, caretOffset: Int): Boolean {
        return element.isBlockComment()
    }

    override fun applyTo(element: PsiComment, editor: Editor?) {
        val psiFactory = KtPsiFactory(element)

        val prevSibling = element.prevSibling
        val indent = if (prevSibling is PsiWhiteSpace) {
            val space = prevSibling.text.reversed().takeWhile { it == ' ' || it == '\t' }
            psiFactory.createWhiteSpace("\n$space")
        } else {
            psiFactory.createNewLine()
        }

        val comments = element.text
            .substring(2, element.text.length - 2)
            .trim()
            .split("\n")
            .reversed()
        val lastIndex = comments.size - 1
        val parent = element.parent
        comments.forEachIndexed { index, comment ->
            val commentText = comment.trim().let { if (it.isEmpty()) "//" else "// $it" }
            parent.addAfter(psiFactory.createComment(commentText), element)
            if (index != lastIndex) parent.addAfter(indent, element)
        }
        element.delete()
    }
}

private fun PsiElement.isBlockComment() = node.elementType == KtTokens.BLOCK_COMMENT
