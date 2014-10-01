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
import com.intellij.lang.ASTNode
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiWhiteSpace
import com.intellij.psi.impl.source.tree.LeafPsiElement
import org.jetbrains.jet.lang.psi.JetBlockExpression
import org.jetbrains.jet.lang.psi.JetWhenExpression
import org.jetbrains.jet.lexer.JetTokens

import java.util.ArrayList
import com.intellij.codeInsight.editorActions.ExtendWordSelectionHandlerBase

/**
 * Originally from IDEA platform: CodeBlockOrInitializerSelectioner
 */
public class JetCodeBlockSelectioner : BasicSelectioner() {
    override fun canSelect(e: PsiElement): Boolean {
        return e is JetBlockExpression || e is JetWhenExpression
    }

    override fun select(e: PsiElement, editorText: CharSequence, cursorOffset: Int, editor: Editor): List<TextRange> {
        val result = ArrayList<TextRange>()

        val children = e.getNode()!!.getChildren(null)

        val start = findOpeningBrace(children)
        val end = findClosingBrace(children, start)

        result.add(e.getTextRange()!!)
        result.addAll(ExtendWordSelectionHandlerBase.expandToWholeLine(editorText, TextRange(start, end)))

        return result
    }

    class object {
        public fun findOpeningBrace(children: Array<ASTNode>): Int {
            var start = children.last().getTextRange()!!.getStartOffset()
            for ((i, child) in children.withIndices()) {
                if (child is LeafPsiElement) {
                    if (child.getElementType() == JetTokens.LBRACE) {
                        var j = i + 1
                        while (children[j] is PsiWhiteSpace) {
                            j++
                        }

                        start = children[j].getTextRange()!!.getStartOffset()
                    }
                }
            }
            return start
        }

        public fun findClosingBrace(children: Array<ASTNode>, startOffset: Int): Int {
            var end = children[children.size - 1].getTextRange()!!.getEndOffset()
            for ((i, child) in children.withIndices()) {
                if (child is LeafPsiElement) {
                    if (child.getElementType() == JetTokens.RBRACE) {
                        var j = i - 1
                        while (children[j] is PsiWhiteSpace && children[j].getTextRange()!!.getStartOffset() > startOffset) {
                            j--
                        }

                        end = children[j].getTextRange()!!.getEndOffset()
                    }
                }
            }
            return end
        }
    }
}
