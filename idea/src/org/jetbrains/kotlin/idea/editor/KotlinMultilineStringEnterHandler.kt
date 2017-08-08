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

package org.jetbrains.kotlin.idea.editor

import com.intellij.codeInsight.CodeInsightSettings
import com.intellij.codeInsight.editorActions.enter.EnterHandlerDelegate
import com.intellij.codeInsight.editorActions.enter.EnterHandlerDelegate.Result
import com.intellij.codeInsight.editorActions.enter.EnterHandlerDelegateAdapter
import com.intellij.injected.editor.EditorWindow
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.actionSystem.EditorActionHandler
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Ref
import com.intellij.openapi.util.TextRange
import com.intellij.openapi.util.text.StringUtil
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.codeStyle.CodeStyleSettingsManager
import com.intellij.psi.impl.source.tree.LeafPsiElement
import org.jetbrains.kotlin.idea.KotlinFileType
import org.jetbrains.kotlin.idea.refactoring.hostEditor
import org.jetbrains.kotlin.idea.refactoring.project
import org.jetbrains.kotlin.idea.refactoring.toPsiFile
import org.jetbrains.kotlin.idea.util.application.runWriteAction
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getCallNameExpression
import org.jetbrains.kotlin.psi.psiUtil.isSingleQuoted
import org.jetbrains.kotlin.psi.psiUtil.parents
import org.jetbrains.kotlin.utils.addToStdlib.firstIsInstanceOrNull

class KotlinMultilineStringEnterHandler : EnterHandlerDelegateAdapter() {
    private var wasInMultilineString: Boolean = false
    private var whiteSpaceAfterCaret: String = ""
    private var isInBrace = false

    override fun preprocessEnter(
            file: PsiFile, editor: Editor, caretOffset: Ref<Int>, caretAdvance: Ref<Int>, dataContext: DataContext,
            originalHandler: EditorActionHandler?): EnterHandlerDelegate.Result {
        val offset = caretOffset.get().toInt()
        if (editor !is EditorWindow) {
            return preprocessEnter(file, editor, offset, originalHandler, dataContext)
        }

        val hostPosition = getHostPosition(dataContext) ?: return Result.Continue
        return preprocessEnter(hostPosition, originalHandler, dataContext)
    }

    private fun preprocessEnter(hostPosition: HostPosition, originalHandler: EditorActionHandler?, dataContext: DataContext): Result {
        val (file, editor, offset) = hostPosition
        return preprocessEnter(file, editor, offset, originalHandler, dataContext)
    }

    private fun preprocessEnter(file: PsiFile, editor: Editor, offset: Int, originalHandler: EditorActionHandler?, dataContext: DataContext): Result {
        if (file !is KtFile) return Result.Continue

        val document = editor.document
        val text = document.text

        if (offset == 0 || offset >= text.length) return Result.Continue

        val element = file.findElementAt(offset)
        if (!inMultilineString(element, offset)) {
            return Result.Continue
        }
        else {
            wasInMultilineString = true
        }

        whiteSpaceAfterCaret = text.substring(offset).takeWhile { ch -> ch == ' ' || ch == '\t' }
        document.deleteString(offset, offset + whiteSpaceAfterCaret.length)

        val ch1 = text[offset - 1]
        val ch2 = text[offset]
        isInBrace = (ch1 == '(' && ch2 == ')') || (ch1 == '{' && ch2 == '}')
        if (!isInBrace || !CodeInsightSettings.getInstance().SMART_INDENT_ON_ENTER) {
            return Result.Continue
        }

        originalHandler?.execute(editor, editor.caretModel.currentCaret, dataContext)
        return Result.DefaultForceIndent
    }

    override fun postProcessEnter(file: PsiFile, editor: Editor, dataContext: DataContext): Result {
        if (editor !is EditorWindow) {
            return postProcessEnter(file, editor)
        }

        val hostPosition = getHostPosition(dataContext) ?: return Result.Continue
        return postProcessEnter(hostPosition.file, hostPosition.editor)
    }

    private fun postProcessEnter(file: PsiFile, editor: Editor): Result {
        if (file !is KtFile) return Result.Continue

        if (!wasInMultilineString) return Result.Continue
        wasInMultilineString = false

        val project = file.project
        val document = editor.document
        PsiDocumentManager.getInstance(project).commitDocument(document)

        val caretModel = editor.caretModel
        val offset = caretModel.offset

        val element = file.findElementAt(offset) ?: return Result.Continue
        val literal = findString(element, offset) ?: return Result.Continue

        val hasTrimIndentCallInChain = hasTrimIndentCallInChain(literal)
        val marginChar = if (hasTrimIndentCallInChain)
            null
        else
            getMarginCharFromTrimMarginCallsInChain(literal) ?: getMarginCharFromLiteral(literal)

        runWriteAction {
            val settings = MultilineSettings(file.project)

            val caretMarker = document.createRangeMarker(offset, offset)
            caretMarker.isGreedyToRight = true
            fun caretOffset(): Int = caretMarker.endOffset

            val prevLineNumber = document.getLineNumber(offset) - 1
            assert(prevLineNumber >= 0)

            val prevLine = getLineByNumber(prevLineNumber, document)
            val currentLine = getLineByNumber(prevLineNumber + 1, document)
            val nextLine = if (document.lineCount > prevLineNumber + 2) getLineByNumber(prevLineNumber + 2, document) else ""

            val wasSingleLine = literal.text.indexOf("\n") == literal.text.lastIndexOf("\n") // Only one '\n' in the string after insertion
            val lines = literal.text.split("\n")

            val literalOffset = literal.textRange.startOffset
            if (wasSingleLine || (lines.size == 3 && isInBrace)) {
                val shouldUseTrimIndent = hasTrimIndentCallInChain || (marginChar == null && lines.first().trim() == MULTILINE_QUOTE)
                val newMarginChar: Char? = if (shouldUseTrimIndent) null else (marginChar ?: DEFAULT_TRIM_MARGIN_CHAR)

                insertTrimCall(document, literal, if (shouldUseTrimIndent) null else newMarginChar)

                val prevIndent = settings.indentLength(prevLine)
                val indentSize = prevIndent + settings.marginIndent

                forceIndent(caretOffset(), indentSize, newMarginChar, document, settings)

                val isInLineEnd = literal.text.substring(offset - literalOffset) == MULTILINE_QUOTE
                if (isInLineEnd) {
                    // Move close quote to next line
                    caretMarker.isGreedyToRight = false
                    insertNewLine(caretOffset(), prevIndent, document, settings)
                    caretMarker.isGreedyToRight = true
                }

                if (isInBrace) {
                    // Move closing bracket under same indent
                    forceIndent(caretOffset() + 1, indentSize, newMarginChar, document, settings)
                }
            }
            else {
                val isPrevLineFirst = document.getLineNumber(literalOffset) == prevLineNumber

                val indentInPreviousLine = when {
                    isPrevLineFirst -> lines.first().substring(MULTILINE_QUOTE.length)
                    else -> prevLine
                }.prefixLength { it == ' ' || it == '\t' }

                val prefixStripped = when {
                    isPrevLineFirst -> lines.first().substring(indentInPreviousLine + MULTILINE_QUOTE.length)
                    else -> prevLine.substring(indentInPreviousLine)
                }

                val nonBlankNotFirstLines = lines.subList(1, lines.size).filterNot { it.isBlank() || it.trimStart() == MULTILINE_QUOTE }

                val marginCharToInsert = if (marginChar != null &&
                    !prefixStripped.startsWith(marginChar) &&
                    !nonBlankNotFirstLines.isEmpty() &&
                    nonBlankNotFirstLines.none { it.trimStart().startsWith(marginChar) }) {

                    // We have margin char but decide not to insert it
                    null
                }
                else {
                    marginChar
                }

                if (marginCharToInsert == null || !currentLine.trimStart().startsWith(marginCharToInsert)) {
                    val indentLength = when {
                        isInBrace -> settings.indentLength(nextLine)
                        !isPrevLineFirst -> settings.indentLength(prevLine)
                        else -> settings.indentLength(prevLine) + settings.marginIndent
                    }

                    forceIndent(caretOffset(), indentLength, marginCharToInsert, document, settings)

                    val wsAfterMargin = when {
                        marginCharToInsert != null && prefixStripped.startsWith(marginCharToInsert) -> {
                            prefixStripped.substring(1).takeWhile { it == ' ' || it == '\t' }
                        }
                        else -> ""
                    }

                    // Insert same indent after margin char that previous line has
                    document.insertString(caretOffset(), wsAfterMargin)

                    if (isInBrace) {
                        val nextLineOffset = document.getLineStartOffset(prevLineNumber + 2)
                        forceIndent(nextLineOffset, 0, null, document, settings)
                        document.insertString(nextLineOffset, (marginCharToInsert?.toString() ?: "") + wsAfterMargin)
                        forceIndent(nextLineOffset, indentLength, null, document, settings)
                    }
                }
            }

            document.insertString(caretOffset(), whiteSpaceAfterCaret)

            caretModel.moveToOffset(caretOffset())
            caretMarker.dispose()
        }

        return Result.Stop
    }

    companion object {
        val DEFAULT_TRIM_MARGIN_CHAR = '|'
        val TRIM_INDENT_CALL = "trimIndent"
        val TRIM_MARGIN_CALL = "trimMargin"

        val MULTILINE_QUOTE = "\"\"\""

        class MultilineSettings(project: Project) {
            private val kotlinIndentOptions =
                    CodeStyleSettingsManager.getInstance(project).currentSettings.getIndentOptions(KotlinFileType.INSTANCE)

            private val useTabs = kotlinIndentOptions.USE_TAB_CHARACTER
            private val tabSize = kotlinIndentOptions.TAB_SIZE
            private val regularIndent = kotlinIndentOptions.INDENT_SIZE
            val marginIndent = regularIndent

            fun getSmartSpaces(count: Int): String = when {
                useTabs -> StringUtil.repeat("\t", count / tabSize) + StringUtil.repeat(" ", count % tabSize)
                else -> StringUtil.repeat(" ", count)
            }

            fun indentLength(line: String): Int = when {
                useTabs -> {
                    val tabsCount = line.prefixLength { it == '\t' }
                    tabsCount * tabSize + line.substring(tabsCount).prefixLength { it == ' ' }
                }
                else -> line.prefixLength { it == ' ' }
            }
        }

        fun findString(element: PsiElement?, offset: Int): KtStringTemplateExpression? {
            if (element !is LeafPsiElement) return null

            when (element.elementType) {
                KtTokens.REGULAR_STRING_PART -> {
                    // Ok
                }
                KtTokens.CLOSING_QUOTE, KtTokens.SHORT_TEMPLATE_ENTRY_START, KtTokens.LONG_TEMPLATE_ENTRY_START -> {
                    if (element.startOffset != offset) {
                        return null
                    }
                }
                else -> return null
            }

            return element.parents.firstIsInstanceOrNull<KtStringTemplateExpression>()
        }

        fun inMultilineString(element: PsiElement?, offset: Int) =
                !(findString(element, offset)?.isSingleQuoted() ?: true)

        fun getMarginCharFromLiteral(str: KtStringTemplateExpression, marginChar: Char = DEFAULT_TRIM_MARGIN_CHAR): Char? {
            val lines = str.text.lines()
            if (lines.size <= 2) return null

            val middleNonBlank = lines.subList(1, lines.size - 1).filter { !it.isBlank() }

            if (middleNonBlank.isNotEmpty() && middleNonBlank.all { it.trimStart().startsWith(marginChar) }) {
                return marginChar
            }

            return null
        }


        private fun getLiteralCalls(str: KtStringTemplateExpression): Sequence<KtCallExpression> {
            var previous: PsiElement = str
            return str.parents
                    .takeWhile { parent ->
                        if (parent is KtQualifiedExpression && parent.receiverExpression == previous) {
                            previous = parent
                            true
                        }
                        else {
                            false
                        }
                    }
                    .mapNotNull { qualified ->
                        (qualified as KtQualifiedExpression).selectorExpression as? KtCallExpression
                    }
        }

        fun getMarginCharFromTrimMarginCallsInChain(str: KtStringTemplateExpression): Char? {
            val literalCall = getLiteralCalls(str).firstOrNull { call ->
                call.getCallNameExpression()?.text == TRIM_MARGIN_CALL
            } ?: return null

            val firstArgument = literalCall.valueArguments.getOrNull(0) ?: return DEFAULT_TRIM_MARGIN_CHAR
            val argumentExpression = firstArgument.getArgumentExpression() as? KtStringTemplateExpression ?: return DEFAULT_TRIM_MARGIN_CHAR
            val entry = argumentExpression.entries.singleOrNull() as? KtLiteralStringTemplateEntry ?: return DEFAULT_TRIM_MARGIN_CHAR

            return entry.text?.singleOrNull() ?: DEFAULT_TRIM_MARGIN_CHAR
        }

        fun hasTrimIndentCallInChain(str: KtStringTemplateExpression): Boolean {
            return getLiteralCalls(str).any { call -> call.getCallNameExpression()?.text == TRIM_INDENT_CALL }
        }

        fun getLineByNumber(number: Int, document: Document): String =
                document.getText(TextRange(document.getLineStartOffset(number), document.getLineEndOffset(number)))

        fun insertNewLine(nlOffset: Int, indent: Int, document: Document, settings: MultilineSettings) {
            document.insertString(nlOffset, "\n")
            forceIndent(nlOffset + 1, indent, null, document, settings)
        }

        fun forceIndent(offset: Int, indent: Int, marginChar: Char?, document: Document, settings: MultilineSettings) {
            val lineNumber = document.getLineNumber(offset)
            val lineStart = document.getLineStartOffset(lineNumber)
            val line = getLineByNumber(lineNumber, document)
            val wsPrefix = line.takeWhile { c -> c == ' ' || c == '\t' }
            document.replaceString(lineStart,
                                   lineStart + wsPrefix.length,
                                   settings.getSmartSpaces(indent) + (marginChar?.toString() ?: ""))
        }

        fun String.prefixLength(f: (Char) -> Boolean) = takeWhile(f).count()

        fun insertTrimCall(document: Document, literal: KtStringTemplateExpression, marginChar: Char?) {
            if (hasTrimIndentCallInChain(literal) || getMarginCharFromTrimMarginCallsInChain(literal) != null) return
            if (literal.parents.any { it is KtAnnotationEntry }) return

            if (marginChar == null) {
                document.insertString(literal.textRange.endOffset, ".$TRIM_INDENT_CALL()")
            }
            else {
                document.insertString(
                        literal.textRange.endOffset,
                        if (marginChar == DEFAULT_TRIM_MARGIN_CHAR) {
                            ".$TRIM_MARGIN_CALL()"
                        }
                        else {
                            ".$TRIM_MARGIN_CALL(\"$marginChar\")"
                        })
            }
        }

        private data class HostPosition(val file: PsiFile, val editor: Editor, val offset: Int)
        private fun getHostPosition(dataContext: DataContext): HostPosition? {
            val editor = dataContext.hostEditor as? EditorEx ?: return null
            val project = dataContext.project

            val virtualFile = editor.virtualFile ?: return null
            val psiFile = virtualFile.toPsiFile(project) ?: return null

            return HostPosition(psiFile, editor, editor.caretModel.offset)
        }
    }
}