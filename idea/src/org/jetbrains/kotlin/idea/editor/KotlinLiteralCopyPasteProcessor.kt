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

import com.intellij.codeInsight.editorActions.CopyPastePreProcessor
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.RawText
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.TextRange
import com.intellij.openapi.util.text.LineTokenizer
import com.intellij.openapi.util.text.StringUtil
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import org.jetbrains.annotations.TestOnly
import org.jetbrains.kotlin.idea.editor.fixers.range
import org.jetbrains.kotlin.lexer.KotlinLexer
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtEscapeStringTemplateEntry
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtStringTemplateExpression
import org.jetbrains.kotlin.psi.psiUtil.*
import kotlin.coroutines.experimental.SequenceBuilder
import kotlin.coroutines.experimental.buildIterator

private val PsiElement.templateContentRange: TextRange?
    get() = this.getParentOfType<KtStringTemplateExpression>(false)?.let{
        it.textRange.cutOut(it.getContentRange())
    }


private fun PsiFile.getTemplateIfAtLiteral(offset: Int): KtStringTemplateExpression? {
    val at = this.findElementAt(offset) ?: return null
    return when (at.node?.elementType) {
        KtTokens.REGULAR_STRING_PART, KtTokens.ESCAPE_SEQUENCE, KtTokens.LONG_TEMPLATE_ENTRY_START, KtTokens.SHORT_TEMPLATE_ENTRY_START -> at.parent.parent as? KtStringTemplateExpression
        KtTokens.CLOSING_QUOTE -> if (offset == at.startOffset) at.parent as? KtStringTemplateExpression else null
        else -> null
    }
}


//Copied from StringLiteralCopyPasteProcessor to avoid erroneous inheritance
private fun deduceBlockSelectionWidth(startOffsets: IntArray, endOffsets: IntArray, text: String): Int {
    val fragmentCount = startOffsets.size
    assert(fragmentCount > 0)
    var totalLength = fragmentCount - 1 // number of line breaks inserted between fragments
    for (i in 0..fragmentCount - 1) {
        totalLength += endOffsets[i] - startOffsets[i]
    }
    if (totalLength < text.length && (text.length + 1) % fragmentCount == 0) {
        return (text.length + 1) / fragmentCount - 1
    }
    else {
        return -1
    }
}

class KotlinLiteralCopyPasteProcessor : CopyPastePreProcessor {
    override fun preprocessOnCopy(file: PsiFile, startOffsets: IntArray, endOffsets: IntArray, text: String): String? {
        if (file !is KtFile) {
            return null
        }
        val buffer = StringBuilder()
        var changed = false
        val fileText = file.text
        val deducedBlockSelectionWidth = deduceBlockSelectionWidth(startOffsets, endOffsets, text)

        for (i in startOffsets.indices) {
            if (i > 0) {
                buffer.append('\n') // LF is added for block selection
            }
            val fileRange = TextRange(startOffsets[i], endOffsets[i])
            var givenTextOffset = fileRange.startOffset
            while (givenTextOffset < fileRange.endOffset) {
                val element: PsiElement? = file.findElementAt(givenTextOffset)
                if (element == null) {
                    buffer.append(fileText.substring(givenTextOffset, fileRange.endOffset))
                    break
                }
                val elTp = element.node.elementType
                if (elTp == KtTokens.ESCAPE_SEQUENCE && fileRange.contains(element.range) &&
                    element.templateContentRange?.contains(fileRange) == true) {
                    val tpEntry = element.parent as KtEscapeStringTemplateEntry
                    changed = true
                    buffer.append(tpEntry.unescapedValue)
                    givenTextOffset = element.endOffset
                }
                else if (elTp == KtTokens.SHORT_TEMPLATE_ENTRY_START || elTp == KtTokens.LONG_TEMPLATE_ENTRY_START) {
                    //Process inner templates without escaping
                    val tpEntry = element.parent
                    val inter = fileRange.intersection(tpEntry.range)!!
                    buffer.append(fileText.substring(inter.startOffset, inter.endOffset))
                    givenTextOffset = inter.endOffset
                }
                else {
                    val inter = fileRange.intersection(element.range)!!
                    buffer.append(fileText.substring(inter.startOffset, inter.endOffset))
                    givenTextOffset = inter.endOffset
                }
            }
            val blockSelectionPadding = deducedBlockSelectionWidth - fileRange.length
            for (j in 0..blockSelectionPadding - 1) {
                buffer.append(' ')
            }
        }

        return if (changed) buffer.toString() else null
    }

    override fun preprocessOnPaste(project: Project, file: PsiFile, editor: Editor, text: String, rawText: RawText?): String {
        if (file !is KtFile) {
            return text
        }
        PsiDocumentManager.getInstance(project).commitDocument(editor.document)
        val selectionModel = editor.selectionModel
        val beginTp = file.getTemplateIfAtLiteral(selectionModel.selectionStart) ?: return text
        val endTp = file.getTemplateIfAtLiteral(selectionModel.selectionEnd) ?: return text
        if (beginTp.isSingleQuoted() != endTp.isSingleQuoted()) {
            return text
        }

        return if (beginTp.isSingleQuoted()) {
            val res = StringBuilder()
            TemplateTokenSequence(text).forEach {
                when (it) {
                    is LiteralChunk -> StringUtil.escapeStringCharacters(it.text.length, it.text, "\$\"", res)
                    is EntryChunk -> res.append(it.text)
                    is NewLineChunk -> res.append("\\n\"+\n \"")
                }
            }
            res.toString()
        }
        else {
            val tripleQuoteRe = Regex("[\"]{3,}")
            TemplateTokenSequence(text).map { chunk ->
                when (chunk) {
                    is LiteralChunk -> chunk.text.replace("\$", "\${'$'}").let { escapedDollar ->
                        tripleQuoteRe.replace(escapedDollar) { "\"\"" + "\${'\"'}".repeat(it.value.count() - 2) }
                    }
                    is EntryChunk -> chunk.text
                    is NewLineChunk -> "\n"
                }
            }.joinToString(separator = "")
        }
    }
}

private sealed class TemplateChunk
private data class LiteralChunk(val text: String) : TemplateChunk()
private data class EntryChunk(val text: String) : TemplateChunk()
private object NewLineChunk : TemplateChunk()

private class TemplateTokenSequence(private val inputString: String) : Sequence<TemplateChunk> {
    private fun String.guessIsTemplateEntryStart(): Boolean = if (this.startsWith("\${")) {
        true
    }
    else if (this.length > 1 && this[0] == '$') {
        val guessedIdentifier = substring(1)
        KotlinLexer().apply { start(guessedIdentifier) }.tokenType == KtTokens.IDENTIFIER
    }
    else {
        false
    }

    private fun findTemplateEntryEnd(input: String, from: Int): Int {
        val wrapped = '"' + input.substring(from) + '"'
        val lexer = KotlinLexer().apply { start(wrapped) }.apply { advance() }

        if (lexer.tokenType == KtTokens.SHORT_TEMPLATE_ENTRY_START) {
            lexer.advance()
            return if (lexer.tokenType == KtTokens.IDENTIFIER) {
                from + lexer.tokenEnd - 1
            }
            else {
                -1
            }
        }
        else if (lexer.tokenType == KtTokens.LONG_TEMPLATE_ENTRY_START) {
            var depth = 0
            while (lexer.tokenType != null) {
                if (lexer.tokenType == KtTokens.LONG_TEMPLATE_ENTRY_START) {
                    depth++
                }
                else if (lexer.tokenType == KtTokens.LONG_TEMPLATE_ENTRY_END) {
                    depth--
                    if (depth == 0) {
                        return from + lexer.currentPosition.offset
                    }
                }
                lexer.advance()
            }
            return -1
        }
        else {
            return -1
        }
    }

    private suspend fun SequenceBuilder<TemplateChunk>.yieldLiteral(chunk: String) {
        val splitLines = LineTokenizer.tokenize(chunk, false, true)
        for (i in 0..splitLines.size - 1) {
            if (i != 0) {
                yield(NewLineChunk)
            }
            splitLines[i].takeIf { !it.isEmpty() }?.let { yield(LiteralChunk(it)) }
        }
    }

    private fun iterTemplateChunks(): Iterator<TemplateChunk> {
        if (inputString.isEmpty()) {
            return emptySequence<TemplateChunk>().iterator()
        }
        return buildIterator {
            var from = 0
            var to = 0
            while (to < inputString.length) {
                val c = inputString[to]
                if (c == '\\') {
                    to += 1
                    if (to < inputString.length) to += 1
                    continue
                }
                else if (c == '$') {
                    if (inputString.substring(to).guessIsTemplateEntryStart()) {
                        if (from < to) yieldLiteral(inputString.substring(from until to))
                        from = to
                        to = findTemplateEntryEnd(inputString, from)
                        if (to != -1) {
                            yield(EntryChunk(inputString.substring(from until to)))
                        }
                        else {
                            to = inputString.length
                            yieldLiteral(inputString.substring(from until to))
                        }
                        from = to
                        continue
                    }
                }
                to++
            }
            if (from < to) {
                yieldLiteral(inputString.substring(from until to))
            }
        }
    }

    override fun iterator(): Iterator<TemplateChunk> = iterTemplateChunks()
}

@TestOnly
internal fun createTemplateSequenceTokenString(input: String): String {
    return TemplateTokenSequence(input).map {
        when (it) {
            is LiteralChunk -> "LITERAL_CHUNK(${it.text})"
            is EntryChunk -> "ENTRY_CHUNK(${it.text})"
            is NewLineChunk -> "NEW_LINE()"
        }
    }.joinToString(separator = "")
}

