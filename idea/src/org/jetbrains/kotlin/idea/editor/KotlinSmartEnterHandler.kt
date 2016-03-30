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

import com.intellij.lang.SmartEnterProcessorWithFixers
import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiWhiteSpace
import com.intellij.psi.codeStyle.CodeStyleSettingsManager
import com.intellij.psi.tree.TokenSet
import com.intellij.util.text.CharArrayUtil
import org.jetbrains.kotlin.KtNodeTypes
import org.jetbrains.kotlin.idea.editor.fixers.*
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getStrictParentOfType

class KotlinSmartEnterHandler: SmartEnterProcessorWithFixers() {
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
                KotlinFunctionDeclarationBodyFixer(),

                KotlinPropertySetterParametersFixer(),
                KotlinPropertySetterBodyFixer(),

                KotlinTryBodyFixer(),
                KotlinCatchParameterFixer(),
                KotlinCatchBodyFixer(),
                KotlinFinallyBodyFixer(),

                KotlinLastLambdaParameterFixer(),

                KotlinClassInitializerFixer()
        )

        addEnterProcessors(KotlinPlainEnterProcessor())
    }

    override fun getStatementAtCaret(editor: Editor?, psiFile: PsiFile?): PsiElement? {
        var atCaret = super.getStatementAtCaret(editor, psiFile)

        if (atCaret is PsiWhiteSpace) return null

        while (atCaret != null) {
            when {
                atCaret.isKotlinStatement() == true -> return atCaret
                atCaret.parent is KtFunctionLiteral -> return atCaret
                atCaret is KtDeclaration -> {
                    val declaration = atCaret
                    when {
                        declaration is KtParameter && !declaration.isInLambdaExpression() -> {/* proceed to function declaration */}
                        declaration.getParent() is KtForExpression -> {/* skip variable declaration in 'for' expression */}
                        else -> return atCaret
                    }
                }
            }

            atCaret = atCaret.parent
        }

        return null
    }

    override fun moveCaretInsideBracesIfAny(editor: Editor, file: PsiFile) {
        var caretOffset = editor.caretModel.offset
        val chars = editor.document.charsSequence

        if (CharArrayUtil.regionMatches(chars, caretOffset, "{}")) {
            caretOffset += 2
        }
        else {
            if (CharArrayUtil.regionMatches(chars, caretOffset, "{\n}")) {
                caretOffset += 3
            }
        }

        caretOffset = CharArrayUtil.shiftBackward(chars, caretOffset - 1, " \t") + 1

        if (CharArrayUtil.regionMatches(chars, caretOffset - "{}".length, "{}") ||
            CharArrayUtil.regionMatches(chars, caretOffset - "{\n}".length, "{\n}")) {
            commit(editor)
            val settings = CodeStyleSettingsManager.getSettings(file.project)
            val old = settings.KEEP_SIMPLE_BLOCKS_IN_ONE_LINE
            settings.KEEP_SIMPLE_BLOCKS_IN_ONE_LINE = false
            val elt = file.findElementAt(caretOffset - 1)!!.getStrictParentOfType<KtBlockExpression>()
            if (elt != null) {
                reformat(elt)
            }
            settings.KEEP_SIMPLE_BLOCKS_IN_ONE_LINE = old
            editor.caretModel.moveToOffset(caretOffset - 1)
        }
    }

    fun registerUnresolvedError(offset: Int) {
        if (myFirstErrorOffset > offset) {
            myFirstErrorOffset = offset
        }
    }

    private fun PsiElement.isKotlinStatement() = when {
        parent is KtBlockExpression && node?.elementType !in BRACES -> true
        parent?.node?.elementType in BRANCH_CONTAINERS && this !is KtBlockExpression -> true
        else -> false
    }

    class KotlinPlainEnterProcessor : SmartEnterProcessorWithFixers.FixEnterProcessor() {
        private fun getControlStatementBlock(caret: Int, element: PsiElement): KtExpression? {
            when (element) {
                is KtDeclarationWithBody -> return element.bodyExpression
                is KtIfExpression -> {
                    if (element.then.isWithCaret(caret)) return element.then
                    if (element.`else`.isWithCaret(caret)) return element.`else`
                }
                is KtLoopExpression -> return element.body
            }

            return null
        }

        override fun doEnter(atCaret: PsiElement, file: PsiFile?, editor: Editor, modified: Boolean): Boolean {
            val block = getControlStatementBlock(editor.caretModel.offset, atCaret) as? KtBlockExpression
            if (block != null) {
                val firstElement = block.firstChild?.nextSibling

                val offset = if (firstElement != null) {
                    firstElement.textRange!!.startOffset - 1
                } else {
                    block.textRange!!.endOffset
                }

                editor.caretModel.moveToOffset(offset)
            }

            plainEnter(editor)
            return true
        }
    }
}

private val BRANCH_CONTAINERS = TokenSet.create(KtNodeTypes.THEN, KtNodeTypes.ELSE, KtNodeTypes.BODY)
private val BRACES = TokenSet.create(KtTokens.RBRACE, KtTokens.LBRACE)
private fun KtParameter.isInLambdaExpression() = this.parent?.parent is KtFunctionLiteral
