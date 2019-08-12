/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.compiler.visualizer

import com.intellij.openapi.util.TextRange
import kotlin.math.max

object Annotator {
    private const val verticalLine = "│"
    private const val comment = "//"

    class AnnotationInfo(val text: String, val range: TextRange)

    private fun putAnnotationToLines(annotations: List<AnnotationInfo>, lineStart: Int, lineSize: Int): Map<Int, StringBuilder> {
        val annotationLines = mutableMapOf(0 to StringBuilder(comment + " ".repeat(lineSize - comment.length)))

        var prevAnnStart = Int.MAX_VALUE
        var lastLevel = 1
        for (ann in annotations) {
            if (ann.range.startOffset + ann.text.length >= prevAnnStart) {
                lastLevel++
            } else {
                lastLevel = 1
            }

            if (!annotationLines.containsKey(lastLevel)) {
                annotationLines[lastLevel] = StringBuilder(comment + " ".repeat(lineSize - comment.length))
            }
            val startReplace = max(comment.length, ann.range.startOffset - lineStart)
            annotationLines[lastLevel] = annotationLines[lastLevel]!!.replace(startReplace, startReplace + ann.text.length, ann.text)

            for (i in 0 until lastLevel) {
                annotationLines[i] = annotationLines[i]!!.replace(startReplace, startReplace + 1, verticalLine)
            }

            prevAnnStart = ann.range.startOffset
        }

        return annotationLines
    }

    fun annotate(text: String, annotation: List<AnnotationInfo>): List<String> {
        val lines = text.lines()
        val resultLines = mutableListOf<String>()
        var lineStartOffset = 0
        for (line in lines) {
            val lineEndOffset = lineStartOffset + line.length
            val annotations = annotation
                .filter { it.range.startOffset in lineStartOffset until lineEndOffset }
                .sortedByDescending { it.range.startOffset }

            if (annotations.isNotEmpty()) {
                val annotationLines = putAnnotationToLines(annotations, lineStartOffset, line.length)
                annotationLines.toSortedMap(Comparator.reverseOrder()).mapTo(resultLines) {
                    it.value.toString()
                }
            }
            resultLines.add(line)

            lineStartOffset = lineEndOffset + 1
        }
        return resultLines
    }
}