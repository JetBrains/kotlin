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

package org.jetbrains.kotlin.idea.completion.handlers

import com.intellij.codeInsight.completion.CompletionInitializationContext
import com.intellij.codeInsight.completion.InsertionContext
import com.intellij.openapi.editor.Document
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiDocumentManager
import org.jetbrains.kotlin.lexer.KtTokens

fun surroundWithBracesIfInStringTemplate(context: InsertionContext) {
    val startOffset = context.startOffset
    val document = context.document
    if (startOffset > 0 && document.charsSequence[startOffset - 1] == '$') {
        val psiDocumentManager = PsiDocumentManager.getInstance(context.project)
        psiDocumentManager.commitAllDocuments()

        if (context.file.findElementAt(startOffset - 1)?.node?.elementType == KtTokens.SHORT_TEMPLATE_ENTRY_START) {
            psiDocumentManager.doPostponedOperationsAndUnblockDocument(document)

            document.insertString(startOffset, "{")
            context.offsetMap.addOffset(CompletionInitializationContext.START_OFFSET, startOffset + 1)

            val tailOffset = context.tailOffset
            document.insertString(tailOffset, "}")
            context.tailOffset = tailOffset
        }
    }
}

fun CharSequence.indexOfSkippingSpace(c: Char, startIndex: Int): Int? {
    for (i in startIndex..this.length() - 1) {
        val currentChar = this[i]
        if (c == currentChar) return i
        if (currentChar != ' ' && currentChar != '\t') return null
    }
    return null
}

fun CharSequence.skipSpaces(index: Int): Int
        = (index..length() - 1).firstOrNull { val c = this[it]; c != ' ' && c != '\t' } ?: this.length()

fun CharSequence.skipSpacesAndLineBreaks(index: Int): Int
        = (index..length() - 1).firstOrNull { val c = this[it]; c != ' ' && c != '\t' && c != '\n' && c != '\r' } ?: this.length()

fun CharSequence.isCharAt(offset: Int, c: Char) = offset < length() && charAt(offset) == c

fun Document.isTextAt(offset: Int, text: String) = offset + text.length() <= getTextLength() && getText(TextRange(offset, offset + text.length())) == text
