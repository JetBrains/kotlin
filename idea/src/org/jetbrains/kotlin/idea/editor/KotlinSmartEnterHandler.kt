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

package org.jetbrains.kotlin.idea.editor

import org.jetbrains.kotlin.idea.editor.fixers.*
import com.intellij.lang.SmartEnterProcessorWithFixers
import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiFile
import com.intellij.util.text.CharArrayUtil
import com.intellij.psi.codeStyle.CodeStyleSettingsManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiWhiteSpace
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtBlockExpression
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtDeclarationWithBody
import org.jetbrains.kotlin.psi.KtIfExpression
import org.jetbrains.kotlin.psi.KtForExpression
import org.jetbrains.kotlin.psi.KtParameter
import org.jetbrains.kotlin.psi.KtFunctionLiteral
import com.intellij.psi.tree.TokenSet
import org.jetbrains.kotlin.KtNodeTypes
import org.jetbrains.kotlin.psi.KtLoopExpression
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.psiUtil.getStrictParentOfType

public class KotlinSmartEnterHandler: SmartEnterProcessorWithFixers() {
    init {
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
                atCaret.isJetStatement() == true -> return atCaret
                atCaret.getParent() is KtFunctionLiteral -> return atCaret
                atCaret is KtDeclaration -> {
                    val declaration = atCaret
                    when {
                        declaration is KtParameter && !declaration.isInLambdaExpression() -> {/* proceed to function declaration */}
                        declaration.getParent() is KtForExpression -> {/* skip variable declaration in 'for' expression */}
                        else -> return atCaret
                    }
                }
            }

            atCaret = atCaret.getParent()
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
            val elt = file.findElementAt(caretOffset - 1)!!.getStrictParentOfType<KtBlockExpression>()
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
        getParent() is KtBlockExpression && getNode()?.getElementType() !in BRACES -> true
        getParent()?.getNode()?.getElementType() in BRANCH_CONTAINERS && this !is KtBlockExpression -> true
        else -> false
    }

    class KotlinPlainEnterProcessor : SmartEnterProcessorWithFixers.FixEnterProcessor() {
        private fun getControlStatementBlock(caret: Int, element: PsiElement): KtExpression? {
            when (element) {
                is KtDeclarationWithBody -> return element.getBodyExpression()
                is KtIfExpression -> {
                    if (element.getThen().isWithCaret(caret)) return element.getThen()
                    if (element.getElse().isWithCaret(caret)) return element.getElse()
                }
                is KtLoopExpression -> return element.getBody()
            }

            return null
        }

        override fun doEnter(atCaret: PsiElement, file: PsiFile?, editor: Editor, modified: Boolean): Boolean {
            val block = getControlStatementBlock(editor.getCaretModel().getOffset(), atCaret) as? KtBlockExpression
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

private val BRANCH_CONTAINERS = TokenSet.create(KtNodeTypes.THEN, KtNodeTypes.ELSE, KtNodeTypes.BODY)
private val BRACES = TokenSet.create(KtTokens.RBRACE, KtTokens.LBRACE)
private fun KtParameter.isInLambdaExpression() = this.getParent()?.getParent() is KtFunctionLiteral
