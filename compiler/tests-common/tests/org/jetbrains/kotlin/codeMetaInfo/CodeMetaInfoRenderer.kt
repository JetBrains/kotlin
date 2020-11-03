/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.codeMetaInfo

import com.intellij.util.containers.Stack
import org.jetbrains.kotlin.checkers.utils.CheckerTestUtil
import org.jetbrains.kotlin.codeMetaInfo.model.CodeMetaInfo
import java.io.File

object CodeMetaInfoRenderer {
    fun renderTagsToText(
        codeMetaInfos: List<CodeMetaInfo>,
        originalText: String
    ): StringBuffer {
        val result = StringBuffer()
        if (codeMetaInfos.isEmpty()) {
            result.append(originalText)
            return result
        }
        val sortedMetaInfos = getSortedCodeMetaInfos(codeMetaInfos)
        val opened = Stack<CodeMetaInfo>()

        for ((i, c) in originalText.withIndex()) {
            checkOpenedAndCloseStringIfNeeded(opened, i, result)
            val matchedCodeMetaInfos = sortedMetaInfos.filter { it.start == i }
            if (matchedCodeMetaInfos.isNotEmpty()) {
                openStartTag(result)
                val iterator = matchedCodeMetaInfos.listIterator()
                var current: CodeMetaInfo? = iterator.next()

                while (current != null) {
                    val next: CodeMetaInfo? = if (iterator.hasNext()) iterator.next() else null
                    opened.push(current)
                    result.append(current.asString())
                    when {
                        next == null ->
                            closeStartTag(result)
                        next.end == current.end ->
                            result.append(", ")
                        else ->
                            closeStartAndOpenNewTag(result)
                    }
                    current = next
                }
            }
            result.append(c)
        }
        checkOpenedAndCloseStringIfNeeded(opened, originalText.length, result)
        return result
    }

    private fun getSortedCodeMetaInfos(metaInfos: Collection<CodeMetaInfo>): List<CodeMetaInfo> {
        return metaInfos.sortedWith(compareBy<CodeMetaInfo> { it.start }.then(compareByDescending { it.end }))
    }

    private fun closeString(result: StringBuffer) = result.append("<!>")
    private fun openStartTag(result: StringBuffer) = result.append("<!")
    private fun closeStartTag(result: StringBuffer) = result.append("!>")
    private fun closeStartAndOpenNewTag(result: StringBuffer) = result.append("!><!")

    private fun checkOpenedAndCloseStringIfNeeded(opened: Stack<CodeMetaInfo>, end: Int, result: StringBuffer) {
        var prev: CodeMetaInfo? = null
        while (!opened.isEmpty() && end == opened.peek().end) {
            if (prev == null || prev.start != opened.peek().start)
                closeString(result)
            prev = opened.pop()
        }
    }
}

fun clearFileFromDiagnosticMarkup(file: File) {
    val text = file.readText()
    val cleanText = clearTextFromDiagnosticMarkup(text)
    file.writeText(cleanText)
}

fun clearTextFromDiagnosticMarkup(text: String): String = CheckerTestUtil.rangeStartOrEndPattern.matcher(text).replaceAll("")
