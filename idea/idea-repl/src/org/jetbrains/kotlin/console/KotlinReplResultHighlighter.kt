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

package org.jetbrains.kotlin.console

import com.intellij.execution.console.BasicGutterContentProvider
import com.intellij.openapi.editor.event.DocumentAdapter
import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.editor.markup.EffectType
import com.intellij.openapi.editor.markup.HighlighterLayer
import com.intellij.openapi.editor.markup.HighlighterTargetArea
import com.intellij.openapi.editor.markup.TextAttributes
import com.intellij.openapi.util.TextRange
import com.intellij.ui.JBColor
import java.util.*
import kotlin.properties.Delegates

public class KotlinReplResultHighlighter(private val editor: EditorEx) : DocumentAdapter() {
    private val EVAL_MARKERS_LENGTH = BasicGutterContentProvider.EVAL_IN_MARKER.length()
    private val ERROR_PREFIX = "Error: "
    private val WARNING_PREFIX = "Warning: "

    var rangeQueue: Queue<TextRange> by Delegates.notNull()
    private var curLine = 0

    private enum class LineType(val color: JBColor) {
        ERROR(JBColor.RED), WARNING(JBColor.YELLOW), USUAL(JBColor.BLACK)
    }

    private fun lineTypeByPrefix(s: String) =
        if (s.startsWith(ERROR_PREFIX))
            LineType.ERROR
        else if (s.startsWith(WARNING_PREFIX))
            LineType.WARNING
        else
            LineType.USUAL

    override fun documentChanged(e: DocumentEvent) {
        val document = editor.document
        val docText = document.text
        if (docText.isEmpty()) {
            curLine = 0
            return
        }

        val lastErrorPos = docText.lastIndexOf(ERROR_PREFIX)
        val lastWarningPos = docText.lastIndexOf(WARNING_PREFIX)
        if (lastErrorPos <= curLine && lastWarningPos <= curLine) return

        val text = document.text
        val totalLines = document.lineCount
        var codeLineOffset = -1
        while (curLine < totalLines) {
            val lineStart = document.getLineStartOffset(curLine)
            val lineEnd = document.getLineEndOffset(curLine)
            val lineText = text.substring(lineStart, lineEnd)
            val lineType = lineTypeByPrefix(lineText)

            if (lineType != LineType.USUAL) {
                if (codeLineOffset == -1) codeLineOffset = document.getLineStartOffset(curLine - 1) + EVAL_MARKERS_LENGTH
                highlightLine(codeLineOffset, lineStart, lineEnd, lineType)
            }
            curLine++
        }

    }

    private fun highlightLine(codeLineOffset: Int, msgLineStart: Int, msgLineEne: Int, lineType: LineType) {
        val historyMarkup = editor.markupModel

        // highlight error or warning message
        val msgTextAttributes = TextAttributes()
        msgTextAttributes.foregroundColor = lineType.color
        msgTextAttributes.backgroundColor = JBColor.LIGHT_GRAY
        historyMarkup.addRangeHighlighter(msgLineStart, msgLineEne, HighlighterLayer.LAST, msgTextAttributes, HighlighterTargetArea.LINES_IN_RANGE)

        // highlight range in [codeLine]
        val range = rangeQueue.poll()
        val highlightedPlaceStart = codeLineOffset + range.startOffset
        val highlightedPlaceSEnd = codeLineOffset + range.endOffset + if (range.endOffset == range.startOffset) 1 else 0

        val errorWaveAttrs = TextAttributes()
        errorWaveAttrs.effectType = EffectType.WAVE_UNDERSCORE
        errorWaveAttrs.effectColor = lineType.color
        historyMarkup.addRangeHighlighter(highlightedPlaceStart, highlightedPlaceSEnd, HighlighterLayer.LAST, errorWaveAttrs, HighlighterTargetArea.EXACT_RANGE)
    }
}