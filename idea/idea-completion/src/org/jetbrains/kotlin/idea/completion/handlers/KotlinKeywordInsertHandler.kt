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

import com.intellij.codeInsight.completion.InsertHandler
import com.intellij.codeInsight.completion.InsertionContext
import com.intellij.codeInsight.lookup.Lookup
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.openapi.project.Project
import com.intellij.psi.codeStyle.CodeStyleManager
import org.jetbrains.kotlin.idea.completion.KeywordLookupObject
import org.jetbrains.kotlin.idea.core.moveCaret
import org.jetbrains.kotlin.lexer.KtTokens.*
import org.jetbrains.kotlin.psi.KtPsiFactory

object KotlinKeywordInsertHandler : InsertHandler<LookupElement> {
    private val NO_SPACE_AFTER = listOf(THIS_KEYWORD,
                                        SUPER_KEYWORD,
                                        FOR_KEYWORD,
                                        NULL_KEYWORD,
                                        TRUE_KEYWORD,
                                        FALSE_KEYWORD,
                                        BREAK_KEYWORD,
                                        CONTINUE_KEYWORD,
                                        IF_KEYWORD,
                                        ELSE_KEYWORD,
                                        WHILE_KEYWORD,
                                        DO_KEYWORD,
                                        TRY_KEYWORD,
                                        WHEN_KEYWORD,
                                        FILE_KEYWORD,
                                        CATCH_KEYWORD,
                                        FINALLY_KEYWORD,
                                        DYNAMIC_KEYWORD,
                                        GET_KEYWORD,
                                        SET_KEYWORD).map { it.value } + "companion object"

    override fun handleInsert(context: InsertionContext, item: LookupElement) {
        val keyword = item.lookupString
        if (keyword !in NO_SPACE_AFTER) {
            WithTailInsertHandler.SPACE.postHandleInsert(context, item)
        }
    }
}

fun createKeywordConstructLookupElement(
        project: Project,
        keyword: String,
        fileTextToReformat: String,
        trimSpacesAroundCaret: Boolean = false,
        showConstructInLookup: Boolean = true
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
    if (indent != null) {
        tailBeforeCaret = tailBeforeCaret.unindent(indent)
        tailAfterCaret = tailAfterCaret.unindent(indent)
    }

    var lookupElementBuilder = LookupElementBuilder.create(KeywordLookupObject(), keyword)
            .bold()
            .withInsertHandler { insertionContext, lookupElement ->
                if (insertionContext.completionChar == Lookup.NORMAL_SELECT_CHAR || insertionContext.completionChar == Lookup.REPLACE_SELECT_CHAR) {
                    val offset = insertionContext.tailOffset
                    val newIndent = detectIndent(insertionContext.document.charsSequence, offset - keyword.length)
                    var beforeCaret = tailBeforeCaret
                    var afterCaret = tailAfterCaret
                    if (newIndent != null) {
                        beforeCaret = beforeCaret.indentLinesAfterFirst(newIndent)
                        afterCaret = afterCaret.indentLinesAfterFirst(newIndent)
                    }
                    insertionContext.document.insertString(offset, beforeCaret + afterCaret)
                    insertionContext.editor.moveCaret(offset + beforeCaret.length)
                }
            }
    if (showConstructInLookup) {
        lookupElementBuilder = lookupElementBuilder.withTailText(tailBeforeCaret + tailAfterCaret)
    }
    return lookupElementBuilder
}

private fun detectIndent(text: CharSequence, offset: Int): String? {
    val reversedIndent = buildString {
        var index = offset - 1
        Loop@
        while (index >= 0) {
            val c = text[index]
            when (c) {
                ' ', '\t' -> append(c)
                '\r', '\n' -> break@Loop
                else -> return null
            }
            index--
        }
    }
    return reversedIndent.reversed()
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

object UseSiteAnnotationTargetInsertHandler : InsertHandler<LookupElement> {
    override fun handleInsert(context: InsertionContext, item: LookupElement) {
        WithTailInsertHandler(":", spaceBefore = false, spaceAfter = false).postHandleInsert(context, item)
    }
}
