/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.compiler.visualizer

import com.intellij.openapi.util.TextRange

object Annotator {
    class AnnotationInfo(val text: String, val globalRange: TextRange)

    class RangeSet<T> {
        data class Rec<T>(val value: T, val rangeInLine: TextRange)
        val localRanges = ArrayList<Rec<T>>()

        private inline fun <U> binarySearch(range: TextRange, found: (Int, Rec<T>?) -> U): U {
            var high = localRanges.size - 1
            var low = 0
            var median = 0
            println("search: $low, $high, ins: $range")
            while (low <= high) {
                median = (high + low) / 2
                val element = localRanges[median]

                println("cmp ${element.rangeInLine}")
                when {
                    range.endOffset < element.rangeInLine.startOffset -> {
                        println("take low, $low, $high")
                        high = median - 1
                    }
                    range.startOffset > element.rangeInLine.endOffset -> {
                        println("take high, $low, $high")
                        low = median + 1
                    }
                    else -> {
                        println("conflict $median")
                        return found(median, element)
                    }
                }
            }
            println("empty at $median")
            return found(median, null)
        }

        fun put(range: TextRange, element: T): Boolean {
            binarySearch(range) { index, conflicting ->
                return if (conflicting == null) {
                    localRanges.add(index, Rec(element, range))
                    true
                } else {
                    false
                }
            }
        }
    }

    private fun tryPutToAnnotationLines(annotationLines: MutableList<RangeSet<String>>, annotation: Pair<AnnotationInfo, TextRange>) {
        val range = annotation.second
        fun buildArrow(from: RangeSet<String>) {
            for (line in annotationLines) {
                if (line == from) {
                    break
                }
                line.put(TextRange(range.startOffset, range.startOffset + 1), "|")
            }
        }

        for (line in annotationLines.drop(1)) {
            if (line.put(range, annotation.first.text)) {
                buildArrow(line)
                return
            }
        }
        annotationLines.add(RangeSet())
        tryPutToAnnotationLines(annotationLines, annotation)
    }

    fun annotate(text: String, annotation: List<AnnotationInfo>): List<String> {
        val lines = text.lines()
        val resultLines = mutableListOf<String>()
        val annotationLines = mutableListOf<RangeSet<String>>()
        var lineStartOffset = 0
        for (line in lines) {
            annotationLines.clear()
            val lineEndOffset = lineStartOffset + line.length
            val annotations =
                annotation.filter { it.globalRange.startOffset in lineStartOffset until lineEndOffset }
                    .sortedByDescending { it.globalRange.startOffset }
                    .map {
                        val startInLine = it.globalRange.startOffset - lineStartOffset
                        it to TextRange(startInLine, startInLine + it.text.length)
                    }
            for (ann in annotations) {
                tryPutToAnnotationLines(annotationLines, ann)
            }
            annotationLines.asReversed().mapTo(resultLines) {
                var prevOffset = 2
                println(it.localRanges)
                it.localRanges.fold("//") { acc, element ->
                    var res = acc
                    res += " ".repeat(element.rangeInLine.startOffset - prevOffset)
                    prevOffset = element.rangeInLine.endOffset
                    res += element.value
                    res
                }
            }
            resultLines.add(line)

            lineStartOffset = lineEndOffset + 1
        }
        return resultLines
    }
}