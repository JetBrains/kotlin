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
import org.jetbrains.kotlin.psi.KtBlockExpression
import org.jetbrains.kotlin.psi.KtWhenExpression
import org.jetbrains.kotlin.lexer.KtTokens

import java.util.ArrayList
import com.intellij.codeInsight.editorActions.ExtendWordSelectionHandlerBase
import org.jetbrains.kotlin.psi.psiUtil.allChildren
import org.jetbrains.kotlin.psi.psiUtil.endOffset
import org.jetbrains.kotlin.psi.psiUtil.startOffset

/**
 * Originally from IDEA platform: CodeBlockOrInitializerSelectioner
 */
class KotlinCodeBlockSelectioner : ExtendWordSelectionHandlerBase() {
    override fun canSelect(e: PsiElement)
            = e is KtBlockExpression || e is KtWhenExpression

    override fun select(e: PsiElement, editorText: CharSequence, cursorOffset: Int, editor: Editor): List<TextRange>? {
        val result = ArrayList<TextRange>()

        val start = findBlockContentStart(e)
        val end = findBlockContentEnd(e)
        if (end > start) {
            result.addAll(ExtendWordSelectionHandlerBase.expandToWholeLine(editorText, TextRange(start, end)))
        }

        result.addAll(ExtendWordSelectionHandlerBase.expandToWholeLine(editorText, e.textRange!!))

        return result
    }

    private fun findBlockContentStart(block: PsiElement): Int {
        val element = block.allChildren
                              .dropWhile { it.node.elementType != KtTokens.LBRACE } // search for '{'
                              .drop(1) // skip it
                              .dropWhile { it is PsiWhiteSpace } // and skip all whitespaces
                              .firstOrNull() ?: block
        return element.startOffset
    }

    private fun findBlockContentEnd(block: PsiElement): Int {
        val element = block.allChildren
                           .toList()
                           .asReversed()
                           .asSequence()
                           .dropWhile { it.node.elementType != KtTokens.RBRACE } // search for '}'
                           .drop(1) // skip it
                           .dropWhile { it is PsiWhiteSpace } // and skip all whitespaces
                           .firstOrNull() ?: block
        return element.endOffset
    }
}
