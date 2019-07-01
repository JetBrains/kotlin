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
import org.jetbrains.kotlin.psi.psiUtil.getNextSiblingIgnoringWhitespace
import org.jetbrains.kotlin.psi.psiUtil.getPrevSiblingIgnoringWhitespace

class ConvertLineCommentToBlockCommentIntention : SelfTargetingIntention<PsiComment>(
    PsiComment::class.java, "Replace with block comment"
) {

    override fun isApplicableTo(element: PsiComment, caretOffset: Int): Boolean {
        return element.isEndOfLineComment()
    }

    override fun applyTo(element: PsiComment, editor: Editor?) {
        var firstComment = element
        while (true) {
            firstComment = firstComment.prevComment() ?: break
        }

        val indent = (firstComment.prevSibling as? PsiWhiteSpace)?.text?.reversed()?.takeWhile { it == ' ' || it == '\t' } ?: ""

        val comments = mutableListOf(firstComment)
        var nextComment = firstComment
        while (true) {
            nextComment = nextComment.nextComment() ?: break
            comments.add(nextComment)
        }

        val blockComment = if (comments.size == 1)
            "/* ${comments.first().commentText()} */"
        else
            comments.joinToString(separator = "\n", prefix = "/*\n", postfix = "\n$indent*/") {
                "$indent${it.commentText()}"
            }

        comments.drop(1).forEach {
            (it.prevSibling as? PsiWhiteSpace)?.delete()
            it.delete()
        }
        firstComment.replace(KtPsiFactory(element).createComment(blockComment))
    }

}

private fun PsiElement.isEndOfLineComment() = node.elementType == KtTokens.EOL_COMMENT

private fun PsiComment.commentText() = text.substring(2).replace("/*", "/ *").replace("*/", "* /").trim()

private fun PsiComment.nextComment(): PsiComment? {
    return (getNextSiblingIgnoringWhitespace() as? PsiComment)?.takeIf { it.isEndOfLineComment() }
}

private fun PsiComment.prevComment(): PsiComment? {
    return (getPrevSiblingIgnoringWhitespace() as? PsiComment)?.takeIf { it.isEndOfLineComment() }
}

