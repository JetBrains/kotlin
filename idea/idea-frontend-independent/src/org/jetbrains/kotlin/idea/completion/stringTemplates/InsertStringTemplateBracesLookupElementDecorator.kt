/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.completion.stringTemplates

import com.intellij.codeInsight.completion.InsertionContext
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementDecorator
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtNameReferenceExpression
import org.jetbrains.kotlin.psi.psiUtil.startOffset

class InsertStringTemplateBracesLookupElementDecorator(lookupElement: LookupElement) : LookupElementDecorator<LookupElement>(lookupElement) {
    override fun handleInsert(context: InsertionContext) {
        val document = context.document
        val startOffset = context.startOffset

        val psiDocumentManager = PsiDocumentManager.getInstance(context.project)
        psiDocumentManager.commitAllDocuments()

        val token = getToken(context.file, document.charsSequence, startOffset)
        val nameRef = token.parent as KtNameReferenceExpression

        document.insertString(nameRef.startOffset, "{")

        val tailOffset = context.tailOffset
        document.insertString(tailOffset, "}")
        context.tailOffset = tailOffset

        super.handleInsert(context)
    }

    private fun getToken(file: PsiFile, charsSequence: CharSequence, startOffset: Int): PsiElement {
        assert(startOffset > 1 && charsSequence[startOffset - 1] == '.')
        val token = file.findElementAt(startOffset - 2)!!
        return if (token.node.elementType == KtTokens.IDENTIFIER || token.node.elementType == KtTokens.THIS_KEYWORD)
            token
        else
            getToken(file, charsSequence, token.startOffset + 1)
    }
}
