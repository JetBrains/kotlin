/*
 * Copyright 2010-2016 JetBrains s.r.o.
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
import org.jetbrains.kotlin.idea.intentions.MergeIfsIntention
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.*

class JoinBlockIntoSingleStatementHandler : JoinRawLinesHandlerDelegate {
    override fun tryJoinRawLines(document: Document, file: PsiFile, start: Int, end: Int): Int {
        if (file !is KtFile) return -1

        if (start == 0) return -1
        val c = document.charsSequence[start]
        val index = if (c == '\n') start - 1 else start

        val brace = file.findElementAt(index)!!
        if (brace.node!!.elementType != KtTokens.LBRACE) return -1

        val block = brace.parent as? KtBlockExpression ?: return -1
        val statement = block.statements.singleOrNull() ?: return -1
        val parent = block.parent
        if (parent !is KtContainerNode && parent !is KtWhenEntry) return -1
        if (block.node.getChildren(KtTokens.COMMENTS).isNotEmpty()) return -1 // otherwise we will loose comments

        // handle nested if's
        val pparent = parent.parent
        if (pparent is KtIfExpression && block == pparent.then && statement is KtIfExpression && statement.`else` == null) {
            // if outer if has else-branch and inner does not have it, do not remove braces otherwise else-branch will belong to different if!
            if (pparent.`else` != null) return -1

            return MergeIfsIntention.applyTo(pparent)
        }

        val newStatement = block.replace(statement)
        return newStatement.textRange!!.startOffset
    }

    override fun tryJoinLines(document: Document, file: PsiFile, start: Int, end: Int)
            = - 1
}
