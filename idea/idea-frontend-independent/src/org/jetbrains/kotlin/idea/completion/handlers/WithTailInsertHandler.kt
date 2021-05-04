/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.completion.handlers

import com.intellij.codeInsight.AutoPopupController
import com.intellij.codeInsight.completion.InsertHandler
import com.intellij.codeInsight.completion.InsertionContext
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.openapi.components.serviceOrNull
import com.intellij.openapi.editor.Document
import com.intellij.psi.PsiDocumentManager


abstract class SmartCompletionTailOffsetProvider {
    abstract fun getTailOffset(context: InsertionContext, item: LookupElement): Int
}

class WithTailInsertHandler(
    val tailText: String,
    val spaceBefore: Boolean,
    val spaceAfter: Boolean,
    val overwriteText: Boolean = true
) : InsertHandler<LookupElement> {
    override fun handleInsert(context: InsertionContext, item: LookupElement) {
        item.handleInsert(context)
        postHandleInsert(context, item)
    }

    fun postHandleInsert(context: InsertionContext, item: LookupElement) {
        val completionChar = context.completionChar
        if (completionChar == tailText.singleOrNull() || (spaceAfter && completionChar == ' ')) {
            context.setAddCompletionChar(false)
        }
        //TODO: what if completion char is different?

        val document = context.document
        PsiDocumentManager.getInstance(context.project).doPostponedOperationsAndUnblockDocument(document)

        var tailOffset = serviceOrNull<SmartCompletionTailOffsetProvider>()?.getTailOffset(context, item)
            ?:context.tailOffset

        val moveCaret = context.editor.caretModel.offset == tailOffset

        //TODO: analyze parenthesis balance to decide whether to replace or not
        var insert = true
        if (overwriteText) {
            var offset = tailOffset
            if (tailText != " ") {
                offset = document.charsSequence.skipSpacesAndLineBreaks(offset)
            }
            if (shouldOverwriteChar(document, offset)) {
                insert = false
                offset += tailText.length
                tailOffset = offset

                if (spaceAfter && document.charsSequence.isCharAt(offset, ' ')) {
                    document.deleteString(offset, offset + 1)
                }
            }
        }

        var textToInsert = ""
        if (insert) {
            textToInsert = tailText
            if (spaceBefore) textToInsert = " " + textToInsert
        }
        if (spaceAfter) textToInsert += " "

        document.insertString(tailOffset, textToInsert)

        if (moveCaret) {
            context.editor.caretModel.moveToOffset(tailOffset + textToInsert.length)

            if (tailText == ",") {
                AutoPopupController.getInstance(context.project)?.autoPopupParameterInfo(context.editor, null)
            }
        }
    }

    private fun shouldOverwriteChar(document: Document, offset: Int): Boolean {
        if (!document.isTextAt(offset, tailText)) return false
        if (tailText == " " && document.charsSequence.isCharAt(offset + 1, '}')) return false // do not overwrite last space before '}'
        return true
    }

    companion object {
        val COMMA = WithTailInsertHandler(",", spaceBefore = false, spaceAfter = true /*TODO: use code style option*/)
        val RPARENTH = WithTailInsertHandler(")", spaceBefore = false, spaceAfter = false)
        val RBRACKET = WithTailInsertHandler("]", spaceBefore = false, spaceAfter = false)
        val RBRACE = WithTailInsertHandler("}", spaceBefore = true, spaceAfter = false)
        val ELSE = WithTailInsertHandler("else", spaceBefore = true, spaceAfter = true)
        val EQ = WithTailInsertHandler("=", spaceBefore = true, spaceAfter = true) /*TODO: use code style options*/
        val SPACE = WithTailInsertHandler(" ", spaceBefore = false, spaceAfter = false, overwriteText = true)
    }
}