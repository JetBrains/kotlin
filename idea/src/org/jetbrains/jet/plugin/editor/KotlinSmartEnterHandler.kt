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

import org.jetbrains.jet.plugin.editor.fixers.*
import com.intellij.lang.SmartEnterProcessorWithFixers
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
import org.jetbrains.jet.lang.psi.JetForExpression
import org.jetbrains.jet.lang.psi.JetParameter
import org.jetbrains.jet.lang.psi.JetFunctionLiteral
import com.intellij.psi.tree.TokenSet
import org.jetbrains.jet.JetNodeTypes
import org.jetbrains.jet.lang.psi.JetLoopExpression
import org.jetbrains.jet.lexer.JetTokens

public class KotlinSmartEnterHandler: SmartEnterProcessorWithFixers() {
    {
        addFixers(
                KotlinIfConditionFixer(),
                KotlinMissingIfBranchFixer(),

                KotlinWhileConditionFixer(),
                KotlinForConditionFixer(),
                KotlinMissingForOrWhileBodyFixer(),

                KotlinWhenSubjectCaretFixer(),
                KotlinMissingWhenBodyFixer(),

                KotlinDoWhileFixer(),

                KotlinFunctionParametersFixer(),
                KotlinFunctionDeclarationBodyFixer()
        )

        addEnterProcessors(KotlinPlainEnterProcessor())
    }

    override fun getStatementAtCaret(editor: Editor?, psiFile: PsiFile?): PsiElement? {
        var atCaret = super.getStatementAtCaret(editor, psiFile)

        if (atCaret is PsiWhiteSpace) return null

        while (atCaret != null) {
            when {
                atCaret?.isJetStatement() == true -> return atCaret
                atCaret?.getParent() is JetFunctionLiteral -> return atCaret
                atCaret is JetDeclaration -> {
                    val declaration = atCaret!!
                    when {
                        declaration is JetParameter && !declaration.isInLambdaExpression() -> {/* proceed to function declaration */}
                        declaration.getParent() is JetForExpression -> {/* skip variable declaration in 'for' expression */}
                        else -> return atCaret
                    }
                }
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
            if (elt != null) {
                reformat(elt)
            }
            settings.KEEP_SIMPLE_BLOCKS_IN_ONE_LINE = old
            editor.getCaretModel().moveToOffset(caretOffset - 1)
        }
    }

    public fun registerUnresolvedError(offset: Int) {
        if (myFirstErrorOffset > offset) {
            myFirstErrorOffset = offset
        }
    }

    private fun PsiElement.isJetStatement() = when {
        getParent() is JetBlockExpression && getNode()?.getElementType() !in BRACES -> true
        getParent()?.getNode()?.getElementType() in BRANCH_CONTAINERS && this !is JetBlockExpression -> true
        else -> false
    }

    class KotlinPlainEnterProcessor : SmartEnterProcessorWithFixers.FixEnterProcessor() {
        private fun getControlStatementBlock(caret: Int, element: PsiElement): JetExpression? {
            when (element) {
                is JetDeclarationWithBody -> return element.getBodyExpression()
                is JetIfExpression -> {
                    if (element.getThen().isWithCaret(caret)) return element.getThen()
                    if (element.getElse().isWithCaret(caret)) return element.getElse()
                }
                is JetLoopExpression -> return element.getBody()
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

private val BRANCH_CONTAINERS = TokenSet.create(JetNodeTypes.THEN, JetNodeTypes.ELSE, JetNodeTypes.BODY)
private val BRACES = TokenSet.create(JetTokens.RBRACE, JetTokens.LBRACE)
private fun JetParameter.isInLambdaExpression() = this.getParent()?.getParent() is JetFunctionLiteral
