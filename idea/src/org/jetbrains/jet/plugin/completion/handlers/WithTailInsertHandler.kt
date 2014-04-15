package org.jetbrains.jet.plugin.completion.handlers

import com.intellij.codeInsight.completion.*
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.openapi.editor.event.*
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiDocumentManager

class WithTailInsertHandler(val tailChar: Char, val spaceAfter: Boolean) : InsertHandler<LookupElement> {
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

        fun isCharAt(offset: Int, c: Char) = offset < document.getTextLength() && document.getText(TextRange(offset, offset + 1))[0] == c

        if (isCharAt(maxChangeOffset, tailChar)) {
            document.deleteString(maxChangeOffset, maxChangeOffset + 1)

            if (spaceAfter && isCharAt(maxChangeOffset, ' ')) {
                document.deleteString(maxChangeOffset, maxChangeOffset + 1)
            }
        }

        val textToInsert = if (spaceAfter) tailChar + " " else tailChar.toString()
        document.insertString(maxChangeOffset, textToInsert)
        if (moveCaret) {
            caretModel.moveToOffset(maxChangeOffset + textToInsert.length)
        }
    }
}