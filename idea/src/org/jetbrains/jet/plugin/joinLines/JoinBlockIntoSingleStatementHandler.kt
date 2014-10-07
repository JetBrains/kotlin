/*
 * Copyright 2010-2014 JetBrains s.r.o.
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

package org.jetbrains.jet.plugin.joinLines

import com.intellij.codeInsight.editorActions.JoinRawLinesHandlerDelegate
import com.intellij.openapi.editor.Document
import com.intellij.psi.PsiFile
import org.jetbrains.jet.lang.psi.JetFile
import org.jetbrains.jet.lexer.JetTokens
import org.jetbrains.jet.lang.psi.JetBlockExpression
import org.jetbrains.jet.lang.psi.JetContainerNode

public class JoinBlockIntoSingleStatementHandler : JoinRawLinesHandlerDelegate {
    override fun tryJoinRawLines(document: Document, file: PsiFile, start: Int, end: Int): Int {
        if (file !is JetFile) return -1

        if (start == 0) return -1
        val brace = file.findElementAt(start - 1)!!
        if (brace.getNode()!!.getElementType() != JetTokens.LBRACE) return -1

        val block = brace.getParent() as? JetBlockExpression ?: return -1
        val statement = block.getStatements().singleOrNull() ?: return -1
        if (block.getParent() !is JetContainerNode) return -1
        if (block.getNode().getChildren(JetTokens.COMMENTS).isNotEmpty()) return -1 // otherwise we will loose comments

        val newStatement = block.replace(statement)
        return newStatement.getTextRange()!!.getStartOffset()
    }

    override fun tryJoinLines(document: Document, file: PsiFile, start: Int, end: Int)
            = - 1
}