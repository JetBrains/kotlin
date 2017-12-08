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

import com.intellij.codeInsight.CodeInsightSettings
import com.intellij.codeInsight.editorActions.enter.EnterAfterUnmatchedBraceHandler
import com.intellij.codeInsight.editorActions.enter.EnterHandlerDelegate
import com.intellij.codeInsight.editorActions.enter.EnterHandlerDelegateAdapter
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.actionSystem.EditorActionHandler
import com.intellij.openapi.util.Ref
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiWhiteSpace
import com.intellij.psi.codeStyle.CodeStyleManager
import com.intellij.psi.tree.TokenSet
import com.intellij.util.IncorrectOperationException
import org.jetbrains.kotlin.idea.codeInsight.CodeInsightUtils
import org.jetbrains.kotlin.idea.completion.handlers.isTextAt
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.*

class KotlinEnterHandler: EnterHandlerDelegateAdapter() {
    companion object {
        private val LOG = Logger.getInstance(KotlinEnterHandler::class.java)
        private val FORCE_INDENT_IN_LAMBDA_AFTER = TokenSet.create(KtTokens.ARROW, KtTokens.LBRACE)
    }

    override fun preprocessEnter(
            file: PsiFile,
            editor: Editor,
            caretOffsetRef: Ref<Int>,
            caretAdvance: Ref<Int>,
            dataContext: DataContext,
            originalHandler: EditorActionHandler?
    ): EnterHandlerDelegate.Result? {
        if (file !is KtFile) return EnterHandlerDelegate.Result.Continue

        if (preprocessEnterInStringLiteral(file, editor, caretOffsetRef, caretAdvance)) {
            return EnterHandlerDelegate.Result.DefaultForceIndent
        }

        if (!CodeInsightSettings.getInstance()!!.SMART_INDENT_ON_ENTER) return EnterHandlerDelegate.Result.Continue

        val document = editor.document
        val text = document.charsSequence
        val caretOffset = caretOffsetRef.get()!!.toInt()

        if (caretOffset !in 0..text.length) return EnterHandlerDelegate.Result.Continue

        val elementAt = file.findElementAt(caretOffset)
        if (elementAt is PsiWhiteSpace && ("\n" in elementAt.getText()!!)) return EnterHandlerDelegate.Result.Continue

        // Indent for LBRACE can be removed after fixing IDEA-124917
        val elementBefore = CodeInsightUtils.getElementAtOffsetIgnoreWhitespaceAfter(file, caretOffset)
        val elementAfter = CodeInsightUtils.getElementAtOffsetIgnoreWhitespaceBefore(file, caretOffset)

        val isAfterLBraceOrArrow = elementBefore != null && elementBefore.node!!.elementType in FORCE_INDENT_IN_LAMBDA_AFTER
        val isBeforeRBrace = elementAfter == null || elementAfter.node!!.elementType == KtTokens.RBRACE

        if (isAfterLBraceOrArrow && isBeforeRBrace && (elementBefore!!.parent is KtFunctionLiteral)) {
            originalHandler?.execute(editor, dataContext)
            PsiDocumentManager.getInstance(file.getProject()).commitDocument(document)

            try {
                CodeStyleManager.getInstance(file.getProject())!!.adjustLineIndent(file, editor.caretModel.offset)
            }
            catch (e: IncorrectOperationException) {
                LOG.error(e)
            }

            return EnterHandlerDelegate.Result.DefaultForceIndent
        }

        return EnterHandlerDelegate.Result.Continue
    }

    // We can't use the core platform logic (EnterInStringLiteralHandler) because it assumes that the string
    // is a single token and the first character of the token is an opening quote. In the case of Kotlin,
    // the opening quote is a separate token and the first character of the string token is just a random letter.
    private fun preprocessEnterInStringLiteral(psiFile: PsiFile,
                                               editor: Editor,
                                               caretOffsetRef: Ref<Int>,
                                               caretAdvanceRef: Ref<Int>): Boolean {
        var caretOffset = caretOffsetRef.get()
        val psiAtOffset = psiFile.findElementAt(caretOffset) ?: return false
        val stringTemplate = psiAtOffset.getStrictParentOfType<KtStringTemplateExpression>() ?: return false
        if (!stringTemplate.isSingleQuoted()) return false
        val tokenType = psiAtOffset.node.elementType
        when (tokenType) {
            KtTokens.CLOSING_QUOTE, KtTokens.REGULAR_STRING_PART, KtTokens.ESCAPE_SEQUENCE,
            KtTokens.SHORT_TEMPLATE_ENTRY_START, KtTokens.LONG_TEMPLATE_ENTRY_START -> {
                val doc = editor.document
                var caretAdvance = 1
                if (stringTemplate.parent is KtDotQualifiedExpression) {
                    doc.insertString(stringTemplate.endOffset, ")")
                    doc.insertString(stringTemplate.startOffset, "(")
                    caretOffset++
                    caretAdvance++
                }
                doc.insertString(caretOffset, "\" + \"")
                caretOffsetRef.set(caretOffset + 3)
                caretAdvanceRef.set(caretAdvance)
                return true
            }
        }
        return false
    }
}

class KotlinConvertToBodyEnterHandler : EnterHandlerDelegateAdapter() {
    override fun preprocessEnter(file: PsiFile, editor: Editor,
                                 caretOffset: Ref<Int>, caretAdvance: Ref<Int>, dataContext: DataContext,
                                 originalHandler: EditorActionHandler?): EnterHandlerDelegate.Result {
        if (file !is KtFile) return EnterHandlerDelegate.Result.Continue
        if (!KotlinEditorOptions.getInstance().isEnableSmartConvertToBody) return EnterHandlerDelegate.Result.Continue
        if (!EnterAfterUnmatchedBraceHandler.isAfterUnmatchedLBrace(editor, caretOffset.get(),
                                                                    file.fileType)) return EnterHandlerDelegate.Result.Continue

        val document = editor.document

        //Let's check if it's special convert to body case.
        val prevOffset = caretOffset.get() - 1
        if (document.isTextAt(prevOffset, "{")) {
            document.commit(file.project)
            val element = file.findElementAt(prevOffset)
            if (element != null && element.matchParents(
                    KtFunctionLiteral::class.java,
                    KtLambdaExpression::class.java,
                    KtDeclarationWithBody::class.java
            )) {
                val declaration = element.parent.parent.parent as KtDeclarationWithBody
                if (!declaration.hasDeclaredReturnType()) return EnterHandlerDelegate.Result.Continue
                val equals = declaration.equalsToken
                if (equals != null) {
                    val startOffset =
                            if (equals.prevSibling is PsiWhiteSpace) equals.prevSibling.startOffset
                            else equals.startOffset
                    val endOffset =
                            if (equals.nextSibling is PsiWhiteSpace) equals.nextSibling.endOffset
                            else equals.endOffset
                    document.replaceString(startOffset, endOffset, " ")
                    val newOffset = caretOffset.get() + 1 + startOffset - endOffset
                    caretOffset.set(newOffset)
                    editor.caretModel.moveToOffset(newOffset)
                    document.commit(file.project)
                }
            }
        }

        return EnterHandlerDelegate.Result.Continue
    }
}
