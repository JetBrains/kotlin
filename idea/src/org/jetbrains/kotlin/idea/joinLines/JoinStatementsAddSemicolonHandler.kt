/*
 * Copyright 2010-2017 JetBrains s.r.o.
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

import com.intellij.codeInsight.editorActions.JoinLinesHandlerDelegate.CANNOT_JOIN
import com.intellij.codeInsight.editorActions.JoinRawLinesHandlerDelegate
import com.intellij.openapi.editor.Document
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import org.jetbrains.kotlin.KtNodeTypes.BLOCK
import org.jetbrains.kotlin.lexer.KtTokens.LBRACE
import org.jetbrains.kotlin.lexer.KtTokens.WHITE_SPACE_OR_COMMENT_BIT_SET
import org.jetbrains.kotlin.parsing.KotlinExpressionParsing
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.psiUtil.siblings

class JoinStatementsAddSemicolonHandler : JoinRawLinesHandlerDelegate {

    override fun tryJoinRawLines(document: Document, file: PsiFile, start: Int, end: Int): Int {
        if (file !is KtFile) return CANNOT_JOIN

        val linebreak = file.findElementAt(start)
                                ?.siblings(forward = true, withItself = true)
                                ?.firstOrNull { it.textContains('\n') }
                        ?: return CANNOT_JOIN

        val parent = linebreak.parent ?: return CANNOT_JOIN
        val element1 = linebreak.firstMaterialSiblingSameLine { prevSibling } ?: return CANNOT_JOIN
        val element2 = linebreak.firstMaterialSiblingSameLine { nextSibling } ?: return CANNOT_JOIN

        if (parent.node.elementType != BLOCK) return CANNOT_JOIN
        if (linebreak.text.count { it == '\n' } > 1) return CANNOT_JOIN
        if (!element1.isStatement()) return CANNOT_JOIN
        if (!element2.isStatement()) return CANNOT_JOIN

        document.replaceString(linebreak.textRange.startOffset, linebreak.textRange.endOffset, " ")
        document.insertString(element1.textRange.endOffset, ";")

        return linebreak.textRange.startOffset + 1
    }

    override fun tryJoinLines(document: Document, file: PsiFile, start: Int, end: Int) = CANNOT_JOIN

    private fun PsiElement.firstMaterialSiblingSameLine(getNext: PsiElement.() -> PsiElement?): PsiElement? {
        var element = this
        do {
            element = element.getNext() ?: return null
            if (element.node.elementType !in WHITE_SPACE_OR_COMMENT_BIT_SET)
                return element
        }
        while (!element.textContains('\n'))

        return null
    }

    private fun PsiElement.isStatement(): Boolean {
        if (this.node.elementType == LBRACE) return false

        var firstSubElement: PsiElement = this
        while (true) firstSubElement = firstSubElement.firstChild ?: break

        // Emulates the `atSet(STATEMENT_FIRST)` check at [KotlinExpressionParsing.parseStatements]
        return firstSubElement.node.elementType in KotlinExpressionParsing.STATEMENT_FIRST
    }
}