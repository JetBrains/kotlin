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
import com.intellij.psi.PsiDocumentManager
import com.intellij.codeInsight.AutoPopupController
import com.intellij.codeInsight.lookup.Lookup
import org.jetbrains.jet.plugin.completion.smart.SmartCompletion
import org.jetbrains.jet.plugin.completion.smart.KEEP_OLD_ARGUMENT_LIST_ON_TAB_KEY

abstract class WithTailInsertHandlerBase : InsertHandler<LookupElement> {
    protected abstract fun insertTail(context: InsertionContext, offset: Int, moveCaret: Boolean)

    override fun handleInsert(context: InsertionContext, item: LookupElement) {
        val document = context.getDocument()

        item.handleInsert(context)
        PsiDocumentManager.getInstance(context.getProject()).doPostponedOperationsAndUnblockDocument(document)

        var tailOffset = context.getTailOffset()
        if (context.getCompletionChar() == Lookup.REPLACE_SELECT_CHAR && item.getUserData(KEEP_OLD_ARGUMENT_LIST_ON_TAB_KEY) != null) {
            val offset = context.getOffsetMap().getOffset(SmartCompletion.OLD_ARGUMENTS_REPLACEMENT_OFFSET)
            if (offset != -1) tailOffset = offset
        }
        insertTail(context, tailOffset, context.getEditor().getCaretModel().getOffset() == tailOffset)
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