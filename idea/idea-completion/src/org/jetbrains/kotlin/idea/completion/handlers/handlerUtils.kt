/*
 * Copyright 2010-2017 JetBrains s.r.o.
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
import com.intellij.codeInsight.lookup.Lookup
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.openapi.editor.Document
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiWhiteSpace
import com.intellij.psi.codeStyle.CodeStyleManager
import org.jetbrains.kotlin.idea.completion.KeywordLookupObject
import org.jetbrains.kotlin.idea.core.moveCaret
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtBlockStringTemplateEntry
import org.jetbrains.kotlin.psi.KtNameReferenceExpression
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.psi.psiUtil.canPlaceAfterSimpleNameEntry
import org.jetbrains.kotlin.psi.psiUtil.getNextSiblingIgnoringWhitespace
import org.jetbrains.kotlin.psi.psiUtil.startOffset

fun surroundWithBracesIfInStringTemplate(context: InsertionContext): Boolean {
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
            return true
        }
    }

    return false
}

fun removeRedundantBracesInStringTemplate(context: InsertionContext) {
    val document = context.document
    val tailOffset = context.tailOffset
    if (document.charsSequence[tailOffset] == '}') {
        val psiDocumentManager = PsiDocumentManager.getInstance(context.project)
        psiDocumentManager.commitAllDocuments()

        val token = context.file.findElementAt(tailOffset)
        if (token != null && token.node.elementType == KtTokens.LONG_TEMPLATE_ENTRY_END) {
            val entry = token.parent as KtBlockStringTemplateEntry
            val nameExpression = entry.expression as? KtNameReferenceExpression ?: return
            if (canPlaceAfterSimpleNameEntry(entry.nextSibling)) {
                context.tailOffset++ // place after '}' otherwise it gets invalidated
                val name = nameExpression.getReferencedName()
                val newEntry = KtPsiFactory(entry).createSimpleNameStringTemplateEntry(name)
                entry.replace(newEntry)
            }
        }
    }
}

fun CharSequence.indexOfSkippingSpace(c: Char, startIndex: Int): Int? {
    for (i in startIndex..this.length - 1) {
        val currentChar = this[i]
        if (c == currentChar) return i
        if (currentChar != ' ' && currentChar != '\t') return null
    }
    return null
}

fun CharSequence.skipSpaces(index: Int): Int = (index..length - 1).firstOrNull { val c = this[it]; c != ' ' && c != '\t' } ?: this.length

fun CharSequence.skipSpacesAndLineBreaks(index: Int): Int =
    (index..length - 1).firstOrNull { val c = this[it]; c != ' ' && c != '\t' && c != '\n' && c != '\r' } ?: this.length

fun CharSequence.isCharAt(offset: Int, c: Char) = offset < length && this[offset] == c

fun Document.isTextAt(offset: Int, text: String) =
    offset + text.length <= textLength && getText(TextRange(offset, offset + text.length)) == text

fun createKeywordConstructLookupElement(
    project: Project,
    keyword: String,
    fileTextToReformat: String,
    trimSpacesAroundCaret: Boolean = false
): LookupElement {
    val file = KtPsiFactory(project).createFile(fileTextToReformat)
    CodeStyleManager.getInstance(project).reformat(file)
    val newFileText = file.text

    val keywordOffset = newFileText.indexOf(keyword)
    assert(keywordOffset >= 0)
    val keywordEndOffset = keywordOffset + keyword.length

    val caretPlaceHolder = "caret"

    val caretOffset = newFileText.indexOf(caretPlaceHolder)
    assert(caretOffset >= 0)
    assert(caretOffset >= keywordEndOffset)

    var tailBeforeCaret = newFileText.substring(keywordEndOffset, caretOffset)
    var tailAfterCaret = newFileText.substring(caretOffset + caretPlaceHolder.length)

    if (trimSpacesAroundCaret) {
        tailBeforeCaret = tailBeforeCaret.trimEnd()
        tailAfterCaret = tailAfterCaret.trimStart()
    }

    val indent = detectIndent(newFileText, keywordOffset)
    tailBeforeCaret = tailBeforeCaret.unindent(indent)
    tailAfterCaret = tailAfterCaret.unindent(indent)

    val tailText = (if (tailBeforeCaret.contains('\n')) tailBeforeCaret.replace("\n", "").trimEnd() else tailBeforeCaret) +
            "..." +
            (if (tailAfterCaret.contains('\n')) tailAfterCaret.replace("\n", "").trimStart() else tailAfterCaret)

    return LookupElementBuilder.create(KeywordLookupObject(), keyword)
        .bold()
        .withTailText(tailText)
        .withInsertHandler { insertionContext, _ ->
            if (insertionContext.completionChar == Lookup.NORMAL_SELECT_CHAR ||
                insertionContext.completionChar == Lookup.REPLACE_SELECT_CHAR ||
                insertionContext.completionChar == Lookup.AUTO_INSERT_SELECT_CHAR
            ) {

                val offset = insertionContext.tailOffset
                val newIndent = detectIndent(insertionContext.document.charsSequence, offset - keyword.length)

                val beforeCaret = tailBeforeCaret.indentLinesAfterFirst(newIndent)
                val afterCaret = tailAfterCaret.indentLinesAfterFirst(newIndent)

                val element = insertionContext.file.findElementAt(offset)

                val sibling = when {
                    element !is PsiWhiteSpace -> element
                    element.textContains('\n') -> null
                    else -> element.getNextSiblingIgnoringWhitespace(true)
                }

                if (sibling != null && beforeCaret.trimStart().startsWith(
                        insertionContext.document.getText(
                            TextRange.from(
                                sibling.startOffset,
                                1
                            )
                        )
                    )
                ) {
                    insertionContext.editor.moveCaret(sibling.startOffset + 1)
                } else {
                    insertionContext.document.insertString(offset, beforeCaret + afterCaret)
                    insertionContext.editor.moveCaret(offset + beforeCaret.length)
                }
            }
        }
}

private fun detectIndent(text: CharSequence, offset: Int): String {
    return text.substring(0, offset)
        .substringAfterLast('\n')
        .takeWhile(Char::isWhitespace)
}

private fun String.indentLinesAfterFirst(indent: String): String {
    val text = this
    return buildString {
        val lines = text.lines()
        for ((index, line) in lines.withIndex()) {
            if (index > 0) append(indent)
            append(line)
            if (index != lines.lastIndex) append('\n')
        }
    }
}

private fun String.unindent(indent: String): String {
    val text = this
    return buildString {
        val lines = text.lines()
        for ((index, line) in lines.withIndex()) {
            append(line.removePrefix(indent))
            if (index != lines.lastIndex) append('\n')
        }
    }
}
