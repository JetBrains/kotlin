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

package org.jetbrains.kotlin.idea.joinLines

import com.intellij.codeInsight.editorActions.JoinRawLinesHandlerDelegate
import com.intellij.openapi.editor.Document
import com.intellij.psi.PsiFile
import org.jetbrains.kotlin.lexer.JetTokens
import org.jetbrains.kotlin.psi.*

public class JoinBlockIntoSingleStatementHandler : JoinRawLinesHandlerDelegate {
    override fun tryJoinRawLines(document: Document, file: PsiFile, start: Int, end: Int): Int {
        if (file !is JetFile) return -1

        if (start == 0) return -1
        val c = document.getCharsSequence()[start]
        val index = if (c == '\n') start - 1 else start

        val brace = file.findElementAt(index)!!
        if (brace.getNode()!!.getElementType() != JetTokens.LBRACE) return -1

        val block = brace.getParent() as? JetBlockExpression ?: return -1
        val statement = block.getStatements().singleOrNull() ?: return -1
        val parent = block.getParent()
        if (parent !is JetContainerNode && parent !is JetWhenEntry) return -1
        if (block.getNode().getChildren(JetTokens.COMMENTS).isNotEmpty()) return -1 // otherwise we will loose comments

        // handle nested if's
        val pparent = parent.getParent()
        if (pparent is JetIfExpression && block == pparent.getThen() && statement is JetIfExpression && statement.getElse() == null) {
            // if outer if has else-branch and inner does not have it, do not remove braces otherwise else-branch will belong to different if!
            if (pparent.getElse() != null) return -1

            val condition1 = pparent.getCondition()
            val condition2 = statement.getCondition()
            val body = statement.getThen()
            if (condition1 != null && condition2 != null && body != null) {
                val newCondition = JetPsiFactory(pparent).createExpressionByPattern("$0 && $1", condition1, condition2)
                condition1.replace(newCondition)
                val newBody = block.replace(body)
                return newBody.getTextRange()!!.getStartOffset()
            }
        }

        val newStatement = block.replace(statement)
        return newStatement.getTextRange()!!.getStartOffset()
    }

    override fun tryJoinLines(document: Document, file: PsiFile, start: Int, end: Int)
            = - 1
}
