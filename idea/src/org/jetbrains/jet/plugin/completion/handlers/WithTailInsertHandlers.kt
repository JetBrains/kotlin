package org.jetbrains.jet.plugin.completion.handlers

import com.intellij.codeInsight.completion.*
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.openapi.editor.*
import com.intellij.openapi.editor.event.*
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiDocumentManager

abstract class WithTailInsertHandlerBase : InsertHandler<LookupElement> {
    protected abstract fun insertTail(document: Document, offset: Int): Int

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

        val moveCaret = caretModel.getOffset() == maxChangeOffset
        val endTailOffset = insertTail(document, maxChangeOffset)
        if (moveCaret) {
            caretModel.moveToOffset(endTailOffset)
        }
    }
}

class WithTailCharInsertHandler(val tailChar: Char, val spaceAfter: Boolean) : WithTailInsertHandlerBase() {
    override fun insertTail(document: Document, offset: Int): Int {
        fun isCharAt(offset: Int, c: Char) = offset < document.getTextLength() && document.getText(TextRange(offset, offset + 1))[0] == c

        if (isCharAt(offset, tailChar)) {
            document.deleteString(offset, offset + 1)

            if (spaceAfter && isCharAt(offset, ' ')) {
                document.deleteString(offset, offset + 1)
            }
        }

        val textToInsert = if (spaceAfter) tailChar + " " else tailChar.toString()
        document.insertString(offset, textToInsert)
        return offset + textToInsert.length
    }
}

class WithTailStringInsertHandler(val tail: String) : WithTailInsertHandlerBase() {
    override fun insertTail(document: Document, offset: Int): Int {
        document.insertString(offset, tail)
        return offset + tail.length
    }
}