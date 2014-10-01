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

package org.jetbrains.jet.plugin.editor.wordSelection

import com.intellij.codeInsight.editorActions.wordSelection.BasicSelectioner
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.util.TextRange
import com.intellij.openapi.util.text.LineTokenizer
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiWhiteSpace
import com.intellij.psi.impl.source.tree.LeafPsiElement
import org.jetbrains.jet.lang.psi.JetBlockExpression
import org.jetbrains.jet.lang.psi.JetExpression
import org.jetbrains.jet.lang.psi.JetWhenEntry
import org.jetbrains.jet.lang.psi.JetWhenExpression
import org.jetbrains.jet.lexer.JetTokens

import java.util.ArrayList
import com.intellij.codeInsight.editorActions.ExtendWordSelectionHandlerBase

/**
 * Originally from IDEA platform: StatementGroupSelectioner
 */
public class JetStatementGroupSelectioner : BasicSelectioner() {
    override fun canSelect(e: PsiElement)
            = e is JetExpression || e is JetWhenEntry

    override fun select(e: PsiElement, editorText: CharSequence, cursorOffset: Int, editor: Editor): List<TextRange> {
        val parent = e.getParent()
        if (parent !is JetBlockExpression && parent !is JetWhenExpression) {
            return listOf()
        }

        var startElement = e
        while (startElement.getPrevSibling() != null) {
            val sibling = startElement.getPrevSibling()

            if (sibling is LeafPsiElement && sibling.getElementType() == JetTokens.LBRACE) break

            if (sibling is PsiWhiteSpace && LineTokenizer.tokenize(sibling.getText()!!.toCharArray(), false).size > 2) break

            startElement = sibling!!
        }

        while (startElement is PsiWhiteSpace) {
            startElement = startElement.getNextSibling()!!
        }

        var endElement = e
        while (endElement.getNextSibling() != null) {
            val sibling = endElement.getNextSibling()

            if (sibling is LeafPsiElement && sibling.getElementType() == JetTokens.RBRACE) break

            if (sibling is PsiWhiteSpace && LineTokenizer.tokenize(sibling.getText()!!.toCharArray(), false).size > 2) break

            endElement = sibling!!
        }

        while (endElement is PsiWhiteSpace) {
            endElement = endElement.getPrevSibling()!!
        }

        return ExtendWordSelectionHandlerBase.expandToWholeLine(editorText, TextRange(startElement.getTextRange()!!.getStartOffset(), endElement.getTextRange()!!.getEndOffset()))
    }
}
