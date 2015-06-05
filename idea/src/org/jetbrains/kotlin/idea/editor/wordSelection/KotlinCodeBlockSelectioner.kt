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

package org.jetbrains.kotlin.idea.editor.wordSelection

import com.intellij.lang.ASTNode
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiWhiteSpace
import org.jetbrains.kotlin.psi.JetBlockExpression
import org.jetbrains.kotlin.psi.JetWhenExpression
import org.jetbrains.kotlin.lexer.JetTokens

import java.util.ArrayList
import com.intellij.codeInsight.editorActions.ExtendWordSelectionHandlerBase

/**
 * Originally from IDEA platform: CodeBlockOrInitializerSelectioner
 */
public class KotlinCodeBlockSelectioner : ExtendWordSelectionHandlerBase() {
    override fun canSelect(e: PsiElement)
            = e is JetBlockExpression || e is JetWhenExpression

    override fun select(e: PsiElement, editorText: CharSequence, cursorOffset: Int, editor: Editor): List<TextRange>? {
        val result = ArrayList<TextRange>()

        val node = e.getNode()!!
        val start = findBlockContentStart(node)
        val end = findBlockContentEnd(node)
        if (end > start) {
            result.addAll(ExtendWordSelectionHandlerBase.expandToWholeLine(editorText, TextRange(start, end)))
        }

        result.addAll(ExtendWordSelectionHandlerBase.expandToWholeLine(editorText, e.getTextRange()!!))

        return result
    }

    private fun findBlockContentStart(blockNode: ASTNode): Int {
        val node = blockNode.getChildren(null)
                .stream()
                .dropWhile { it.getElementType() != JetTokens.LBRACE } // search for '{'
                .drop(1) // skip it
                .dropWhile { it is PsiWhiteSpace } // and skip all whitespaces
                .firstOrNull() ?: blockNode
        return node.getTextRange()!!.getStartOffset()
    }

    private fun findBlockContentEnd(blockNode: ASTNode): Int {
        val node = blockNode.getChildren(null)
                           .reverse()
                           .stream()
                           .dropWhile { it.getElementType() != JetTokens.RBRACE } // search for '}'
                           .drop(1) // skip it
                           .dropWhile { it is PsiWhiteSpace } // and skip all whitespaces
                           .firstOrNull() ?: blockNode
        return node.getTextRange()!!.getEndOffset()
    }
}
