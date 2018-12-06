/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.editor

import com.intellij.codeInsight.CodeInsightSettings
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
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtDotQualifiedExpression
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtFunctionLiteral
import org.jetbrains.kotlin.psi.KtStringTemplateExpression
import org.jetbrains.kotlin.psi.psiUtil.endOffset
import org.jetbrains.kotlin.psi.psiUtil.getStrictParentOfType
import org.jetbrains.kotlin.psi.psiUtil.isSingleQuoted
import org.jetbrains.kotlin.psi.psiUtil.startOffset

class KotlinEnterHandler : EnterHandlerDelegateAdapter() {
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
            originalHandler?.execute(editor, editor.caretModel.currentCaret, dataContext)
            PsiDocumentManager.getInstance(file.getProject()).commitDocument(document)

            try {
                CodeStyleManager.getInstance(file.getProject())!!.adjustLineIndent(file, editor.caretModel.offset)
            } catch (e: IncorrectOperationException) {
                LOG.error(e)
            }

            return EnterHandlerDelegate.Result.DefaultForceIndent
        }

        return EnterHandlerDelegate.Result.Continue
    }

    // We can't use the core platform logic (EnterInStringLiteralHandler) because it assumes that the string
    // is a single token and the first character of the token is an opening quote. In the case of Kotlin,
    // the opening quote is a separate token and the first character of the string token is just a random letter.
    private fun preprocessEnterInStringLiteral(
        psiFile: PsiFile,
        editor: Editor,
        caretOffsetRef: Ref<Int>,
        caretAdvanceRef: Ref<Int>
    ): Boolean {
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
