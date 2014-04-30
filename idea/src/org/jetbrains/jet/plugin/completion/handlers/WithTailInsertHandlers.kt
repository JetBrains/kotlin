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

package org.jetbrains.jet.plugin.completion.handlers

import com.intellij.codeInsight.completion.*
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.openapi.editor.event.*
import com.intellij.psi.PsiDocumentManager
import com.intellij.codeInsight.AutoPopupController

abstract class WithTailInsertHandlerBase : InsertHandler<LookupElement> {
    protected abstract fun insertTail(context: InsertionContext, offset: Int, moveCaret: Boolean)

    override fun handleInsert(context: InsertionContext, item: LookupElement) {
        val document = context.getDocument()
        val caretModel = context.getEditor().getCaretModel()

        var maxChangeOffset = caretModel.getOffset()
        val documentListener = object: DocumentListener {
            override fun documentChanged(event: DocumentEvent) {
                val oldEndOffset = event.getOffset() + event.getOldLength()
                if (oldEndOffset < maxChangeOffset) {
                    maxChangeOffset += event.getNewLength() - event.getOldLength()
                }
                else {
                    maxChangeOffset = event.getOffset() + event.getNewLength()
                }
            }

            override fun beforeDocumentChange(event: DocumentEvent) {
            }
        }

        document.addDocumentListener(documentListener)
        try{
            item.handleInsert(context)
            PsiDocumentManager.getInstance(context.getProject()).doPostponedOperationsAndUnblockDocument(document)
        }
        finally {
            document.removeDocumentListener(documentListener)
        }

        insertTail(context, maxChangeOffset, caretModel.getOffset() == maxChangeOffset)
    }
}

class WithTailCharInsertHandler(val tailChar: Char, val spaceAfter: Boolean) : WithTailInsertHandlerBase() {
    override fun insertTail(context: InsertionContext, offset: Int, moveCaret: Boolean) {
        val document = context.getDocument()
        fun isCharAt(offset: Int, c: Char) = offset < document.getTextLength() && document.getCharsSequence().charAt(offset) == c

        if (isCharAt(offset, tailChar)) {
            document.deleteString(offset, offset + 1)

            if (spaceAfter && isCharAt(offset, ' ')) {
                document.deleteString(offset, offset + 1)
            }
        }

        val textToInsert = if (spaceAfter) tailChar + " " else tailChar.toString()
        document.insertString(offset, textToInsert)
        if (moveCaret) {
            context.getEditor().getCaretModel().moveToOffset(offset + textToInsert.length)

            if (tailChar == ',') {
                AutoPopupController.getInstance(context.getProject())?.autoPopupParameterInfo(context.getEditor(), null)
            }
        }
    }
}

class WithTailStringInsertHandler(val tail: String) : WithTailInsertHandlerBase() {
    override fun insertTail(context: InsertionContext, offset: Int, moveCaret: Boolean) {
        context.getDocument().insertString(offset, tail)
        if (moveCaret) {
            context.getEditor().getCaretModel().moveToOffset(offset + tail.length)
        }
    }
}