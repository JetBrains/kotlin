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

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiWhiteSpace
import com.intellij.psi.impl.source.tree.LeafPsiElement
import org.jetbrains.kotlin.psi.JetBlockExpression
import org.jetbrains.kotlin.psi.JetExpression
import org.jetbrains.kotlin.psi.JetWhenEntry
import org.jetbrains.kotlin.psi.JetWhenExpression
import org.jetbrains.kotlin.lexer.JetTokens

import com.intellij.codeInsight.editorActions.ExtendWordSelectionHandlerBase
import org.jetbrains.kotlin.psi.psiUtil.siblings
import com.intellij.psi.PsiComment

/**
 * Originally from IDEA platform: StatementGroupSelectioner
 */
public class KotlinStatementGroupSelectioner : ExtendWordSelectionHandlerBase() {
    override fun canSelect(e: PsiElement): Boolean {
        if (e !is JetExpression && e !is JetWhenEntry && e !is PsiComment) return false
        val parent = e.getParent()
        return parent is JetBlockExpression || parent is JetWhenExpression
    }

    override fun select(e: PsiElement, editorText: CharSequence, cursorOffset: Int, editor: Editor): List<TextRange>? {
        val parent = e.getParent()

        val startElement = e.siblings(forward = false, withItself = false)
                .firstOrNull { // find preceding '{' or blank line
                    it is LeafPsiElement && it.getElementType() == JetTokens.LBRACE ||
                        it is PsiWhiteSpace && it.getText()!!.count { it == '\n' } > 1
                }
                ?.siblings(forward = true, withItself = false)
                ?.dropWhile { it is PsiWhiteSpace } // and take first non-whitespace element after it
                ?.firstOrNull() ?: parent.getFirstChild()!!

        val endElement = e.siblings(forward = true, withItself = false)
                .firstOrNull { // find next '}' or blank line
                    it is LeafPsiElement && it.getElementType() == JetTokens.RBRACE ||
                        it is PsiWhiteSpace && it.getText()!!.count { it == '\n' } > 1
                }
                ?.siblings(forward = false, withItself = false)
                ?.dropWhile { it is PsiWhiteSpace } // and take first non-whitespace element before it
                ?.firstOrNull() ?: parent.getLastChild()!!

        return ExtendWordSelectionHandlerBase.expandToWholeLine(editorText, TextRange(startElement.getTextRange()!!.getStartOffset(), endElement.getTextRange()!!.getEndOffset()))
    }
}
