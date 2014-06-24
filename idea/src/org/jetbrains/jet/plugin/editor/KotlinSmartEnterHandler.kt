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

package org.jetbrains.jet.plugin.editor

import com.intellij.lang.SmartEnterProcessorWithFixers
import org.jetbrains.jet.plugin.editor.fixers.KotlinIfConditionFixer
import org.jetbrains.jet.plugin.editor.fixers.KotlinMissingIfBranchFixer
import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiFile
import com.intellij.util.text.CharArrayUtil
import com.intellij.psi.codeStyle.CodeStyleSettingsManager
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiWhiteSpace
import org.jetbrains.jet.lang.psi.JetDeclaration
import org.jetbrains.jet.lang.psi.JetBlockExpression
import org.jetbrains.jet.lang.psi.JetExpression
import org.jetbrains.jet.lang.psi.JetDeclarationWithBody
import org.jetbrains.jet.lang.psi.JetIfExpression
import org.jetbrains.jet.plugin.editor.fixers.KotlinWhileConditionFixer
import org.jetbrains.jet.lang.psi.JetWhileExpression
import org.jetbrains.jet.plugin.editor.fixers.isWithCaret
import org.jetbrains.jet.plugin.editor.fixers.KotlinForConditionFixer
import org.jetbrains.jet.plugin.editor.fixers.KotlinMissingForOrWhileBodyFixer
import org.jetbrains.jet.lang.psi.JetForExpression
import com.intellij.psi.tree.TokenSet
import org.jetbrains.jet.JetNodeTypes

public class KotlinSmartEnterHandler: SmartEnterProcessorWithFixers() {
    {
        addFixers(
                KotlinIfConditionFixer(),
                KotlinMissingIfBranchFixer(),

                KotlinWhileConditionFixer(),
                KotlinForConditionFixer(),
                KotlinMissingForOrWhileBodyFixer()
        )

        addEnterProcessors(KotlinPlainEnterProcessor())
    }

    override fun getStatementAtCaret(editor: Editor?, psiFile: PsiFile?): PsiElement? {
        var atCaret = super.getStatementAtCaret(editor, psiFile)

        if (atCaret is PsiWhiteSpace) return null

        while (atCaret != null) {
            if (atCaret?.isJetStatement() == true) return atCaret

            if (atCaret is JetDeclaration && (atCaret?.getParent() !is JetForExpression)) {
                return atCaret
            }

            atCaret = atCaret?.getParent()
        }

        return null
    }

    override fun moveCaretInsideBracesIfAny(editor: Editor, file: PsiFile) {
        var caretOffset = editor.getCaretModel().getOffset()
        val chars = editor.getDocument().getCharsSequence()

        if (CharArrayUtil.regionMatches(chars, caretOffset, "{}")) {
            caretOffset += 2
        }
        else {
            if (CharArrayUtil.regionMatches(chars, caretOffset, "{\n}")) {
                caretOffset += 3
            }
        }

        caretOffset = CharArrayUtil.shiftBackward(chars, caretOffset - 1, " \t") + 1

        if (CharArrayUtil.regionMatches(chars, caretOffset - "{}".length(), "{}") ||
                CharArrayUtil.regionMatches(chars, caretOffset - "{\n}".length(), "{\n}")) {
            commit(editor)
            val settings = CodeStyleSettingsManager.getSettings(file.getProject())
            val old = settings.KEEP_SIMPLE_BLOCKS_IN_ONE_LINE
            settings.KEEP_SIMPLE_BLOCKS_IN_ONE_LINE = false
            val elt = PsiTreeUtil.getParentOfType(file.findElementAt(caretOffset - 1), javaClass<JetBlockExpression>())
            reformat(elt)
            settings.KEEP_SIMPLE_BLOCKS_IN_ONE_LINE = old
            editor.getCaretModel().moveToOffset(caretOffset - 1)
        }
    }

    public fun registerUnresolvedError(offset: Int) {
        if (myFirstErrorOffset > offset) {
            myFirstErrorOffset = offset
        }
    }

    private fun PsiElement.isJetStatement() =
        getParent() is JetBlockExpression || (getParent()?.getNode()?.getElementType() in IF_BRANCHES_CONTAINERS)

    class KotlinPlainEnterProcessor : SmartEnterProcessorWithFixers.FixEnterProcessor() {
        private fun getControlStatementBlock(caret: Int, element: PsiElement): JetExpression? {
            when (element) {
                is JetDeclarationWithBody -> return element.getBodyExpression()
                is JetIfExpression -> {
                    if (element.getThen().isWithCaret(caret)) return element.getThen()
                    if (element.getElse().isWithCaret(caret)) return element.getElse()
                }
                is JetWhileExpression -> {
                    if (element.getBody()?.getTextRange()?.contains(caret) == true) return element.getBody()
                }
            }

            return null
        }

        override fun doEnter(atCaret: PsiElement, file: PsiFile?, editor: Editor, modified: Boolean): Boolean {
            val block = getControlStatementBlock(editor.getCaretModel().getOffset(), atCaret) as? JetBlockExpression
            if (block != null) {
                val firstElement = block.getFirstChild()?.getNextSibling()

                val offset = if (firstElement != null) {
                    firstElement.getTextRange()!!.getStartOffset() - 1
                } else {
                    block.getTextRange()!!.getEndOffset()
                }

                editor.getCaretModel().moveToOffset(offset)
            }

            plainEnter(editor)
            return true
        }
    }
}

private val IF_BRANCHES_CONTAINERS = TokenSet.create(JetNodeTypes.THEN, JetNodeTypes.ELSE)