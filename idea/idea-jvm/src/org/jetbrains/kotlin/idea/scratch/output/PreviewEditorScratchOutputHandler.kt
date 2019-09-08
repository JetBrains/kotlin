/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.scratch.output

import com.intellij.diff.util.DiffUtil
import com.intellij.openapi.application.TransactionGuard
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.FoldRegion
import com.intellij.openapi.editor.FoldingModel
import com.intellij.openapi.editor.markup.HighlighterLayer
import com.intellij.openapi.editor.markup.HighlighterTargetArea
import com.intellij.openapi.editor.markup.MarkupModel
import com.intellij.openapi.fileEditor.TextEditor
import com.intellij.openapi.util.text.StringUtil
import org.jetbrains.kotlin.idea.scratch.ScratchExpression
import org.jetbrains.kotlin.idea.scratch.ScratchFile
import java.util.*
import kotlin.math.max

/**
 * Output handler to print scratch output to separate [previewTextEditor] window.
 *
 * Multiline outputs from single expressions are folded.
 */
class PreviewEditorScratchOutputHandler(
    private val previewTextEditor: TextEditor,
    private val toolwindowHandler: ScratchOutputHandler
) : ScratchOutputHandler {
    private val previewOutputBlocksManager: PreviewOutputBlocksManager = PreviewOutputBlocksManager(previewTextEditor.editor)

    /**
     * Returns pairs of line numbers which should be on the same visual positions during scrolling.
     */
    val sourceToPreviewAlignments: Sequence<Pair<Int, Int>> get() = previewOutputBlocksManager.alignments

    override fun onStart(file: ScratchFile) {
        toolwindowHandler.onStart(file)
    }

    override fun handle(file: ScratchFile, expression: ScratchExpression, output: ScratchOutput) {
        printToPreviewEditor(expression, output)
    }

    override fun error(file: ScratchFile, message: String) {
        toolwindowHandler.error(file, message)
    }

    override fun onFinish(file: ScratchFile) {
        toolwindowHandler.onFinish(file)
    }

    override fun clear(file: ScratchFile) {
        toolwindowHandler.clear(file)

        previewOutputBlocksManager.clear()

        clearPreviewEditor()
    }

    private fun printToPreviewEditor(expression: ScratchExpression, output: ScratchOutput) {
        TransactionGuard.submitTransaction(previewTextEditor, Runnable {
            val targetCell = previewOutputBlocksManager.getBlock(expression) ?: previewOutputBlocksManager.addBlockToTheEnd(expression)
            targetCell.addOutput(output)
        })
    }

    private fun clearPreviewEditor() {
        TransactionGuard.submitTransaction(previewTextEditor, Runnable {
            runWriteAction {
                executeCommand {
                    previewTextEditor.editor.document.setText("")
                }
            }
        })
    }
}

private val ScratchExpression.height: Int get() = lineEnd - lineStart + 1

class PreviewOutputBlocksManager(editor: Editor) {
    val targetDocument: Document = editor.document
    val foldingModel: FoldingModel = editor.foldingModel
    val markupModel: MarkupModel = editor.markupModel

    val blocks: NavigableMap<ScratchExpression, OutputBlock> = TreeMap(Comparator.comparingInt { it.lineStart })

    val alignments: Sequence<Pair<Int, Int>> get() = blocks.values.asSequence().map { it.sourceExpression.lineStart to it.lineStart }

    fun getBlock(expression: ScratchExpression): OutputBlock? = blocks[expression]

    fun addBlockToTheEnd(expression: ScratchExpression): OutputBlock = OutputBlock(expression).also {
        if (blocks.putIfAbsent(expression, it) != null) {
            error("There is already a cell for $expression!")
        }
    }

    fun clear() {
        blocks.clear()
    }

    inner class OutputBlock(val sourceExpression: ScratchExpression) {
        private val outputs: MutableList<ScratchOutput> = mutableListOf()

        var lineStart: Int = computeCellLineStart(sourceExpression)
            private set

        val lineEnd: Int get() = lineStart + countNewLines(outputs)
        val height: Int get() = lineEnd - lineStart + 1

        private var foldRegion: FoldRegion? = null

        fun addOutput(output: ScratchOutput) {
            printAndSaveOutput(output)

            blocks.tailMap(sourceExpression).values.forEach {
                it.recalculatePosition()
                it.updateFolding()
            }
        }

        private fun printAndSaveOutput(output: ScratchOutput) {
            val beforeAdding = lineEnd
            val currentOutputStartLine = if (outputs.isEmpty()) lineStart else beforeAdding + 1

            outputs.add(output)

            runWriteAction {
                executeCommand {
                    targetDocument.insertStringAtLine(currentOutputStartLine, output.text)
                }
            }

            val insertedTextStart = targetDocument.getLineStartOffset(currentOutputStartLine)
            val insertedTextEnd = targetDocument.getLineEndOffset(lineEnd)
            colorRange(insertedTextStart, insertedTextEnd, output.type)
        }

        private fun colorRange(startOffset: Int, endOffset: Int, outputType: ScratchOutputType) {
            val textAttributes = getAttributesForOutputType(outputType)

            markupModel.addRangeHighlighter(
                startOffset,
                endOffset,
                HighlighterLayer.SYNTAX,
                textAttributes,
                HighlighterTargetArea.EXACT_RANGE
            )
        }

        private fun recalculatePosition() {
            lineStart = computeCellLineStart(sourceExpression)
        }

        private fun updateFolding() {
            foldingModel.runBatchFoldingOperation {
                foldRegion?.let(foldingModel::removeFoldRegion)

                if (height <= sourceExpression.height) return@runBatchFoldingOperation

                val firstFoldedLine = lineStart + (sourceExpression.height - 1)
                val placeholderLine = "${targetDocument.getLineContent(firstFoldedLine)}..."

                foldRegion = foldingModel.addFoldRegion(
                    targetDocument.getLineStartOffset(firstFoldedLine),
                    targetDocument.getLineEndOffset(lineEnd),
                    placeholderLine
                )

                foldRegion?.isExpanded = isLastCell && isOutputSmall
            }
        }

        private val isLastCell: Boolean get() = false // blocks.higherEntry(sourceExpression) == null
        private val isOutputSmall: Boolean get() = true
    }

    private fun computeCellLineStart(scratchExpression: ScratchExpression): Int {
        val previous = blocks.lowerEntry(scratchExpression)?.value ?: return scratchExpression.lineStart

        val distanceBetweenSources = scratchExpression.lineStart - previous.sourceExpression.lineEnd
        val differenceBetweenSourceAndOutputHeight = previous.sourceExpression.height - previous.height
        val compensation = max(differenceBetweenSourceAndOutputHeight, 0)
        return previous.lineEnd + compensation + distanceBetweenSources
    }
}

private fun countNewLines(list: List<ScratchOutput>) = list.sumBy { StringUtil.countNewLines(it.text) } + max(list.size - 1, 0)

private fun Document.getLineContent(lineNumber: Int) =
    DiffUtil.getLinesContent(this, lineNumber, lineNumber + 1).toString()

fun Document.insertStringAtLine(lineNumber: Int, text: String) {
    while (DiffUtil.getLineCount(this) <= lineNumber) {
        insertString(textLength, "\n")
    }

    insertString(getLineStartOffset(lineNumber), text)
}
