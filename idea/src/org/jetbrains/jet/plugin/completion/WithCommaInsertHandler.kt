package org.jetbrains.jet.plugin.completion

import com.intellij.codeInsight.lookup.*
import com.intellij.codeInsight.completion.*
import com.intellij.openapi.editor.event.DocumentListener
import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.util.TextRange

object WithCommaInsertHandler : InsertHandler<LookupElement> {
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
        item.handleInsert(context)
        document.removeDocumentListener(documentListener)

        val moveCaret = caretModel.getOffset() == maxChangeOffset

        if (maxChangeOffset < document.getTextLength() && document.getText(TextRange(maxChangeOffset, maxChangeOffset + 1)) == ",") {
            document.deleteString(maxChangeOffset, maxChangeOffset + 1)

            if (maxChangeOffset < document.getTextLength() && document.getText(TextRange(maxChangeOffset, maxChangeOffset + 1)) == " ") {
                document.deleteString(maxChangeOffset, maxChangeOffset + 1)
            }
        }

        val textToInsert = ", " //TODO: code style option
        document.insertString(maxChangeOffset, textToInsert)
        if (moveCaret) {
            caretModel.moveToOffset(maxChangeOffset + textToInsert.length)
        }
    }
}