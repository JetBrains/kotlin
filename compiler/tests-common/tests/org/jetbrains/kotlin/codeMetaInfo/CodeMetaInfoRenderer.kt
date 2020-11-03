/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.codeMetaInfo

import com.intellij.util.containers.Stack
import org.jetbrains.kotlin.checkers.utils.CheckerTestUtil
import org.jetbrains.kotlin.codeMetaInfo.model.CodeMetaInfo
import java.io.File
import java.lang.StringBuilder

object CodeMetaInfoRenderer {
    fun renderTagsToText(
        codeMetaInfos: List<CodeMetaInfo>,
        originalText: String
    ): StringBuilder {
        return StringBuilder().apply {
            renderTagsToText(this, codeMetaInfos, originalText)
        }
    }

    fun renderTagsToText(
        builder: StringBuilder,
        codeMetaInfos: List<CodeMetaInfo>,
        originalText: String
    ) {
        if (codeMetaInfos.isEmpty()) {
            builder.append(originalText)
            return
        }
        val sortedMetaInfos = getSortedCodeMetaInfos(codeMetaInfos)
        val opened = Stack<CodeMetaInfo>()

        for ((i, c) in originalText.withIndex()) {
            checkOpenedAndCloseStringIfNeeded(opened, i, builder)
            val matchedCodeMetaInfos = sortedMetaInfos.filter { it.start == i }
            if (matchedCodeMetaInfos.isNotEmpty()) {
                openStartTag(builder)
                val iterator = matchedCodeMetaInfos.listIterator()
                var current: CodeMetaInfo? = iterator.next()

                while (current != null) {
                    val next: CodeMetaInfo? = if (iterator.hasNext()) iterator.next() else null
                    opened.push(current)
                    builder.append(current.asString())
                    when {
                        next == null ->
                            closeStartTag(builder)
                        next.end == current.end ->
                            builder.append(", ")
                        else ->
                            closeStartAndOpenNewTag(builder)
                    }
                    current = next
                }
            }
            builder.append(c)
        }
        checkOpenedAndCloseStringIfNeeded(opened, originalText.length, builder)
    }

    private fun getSortedCodeMetaInfos(metaInfos: Collection<CodeMetaInfo>): List<CodeMetaInfo> {
        return metaInfos.sortedWith(compareBy<CodeMetaInfo> { it.start }.then(compareByDescending { it.end }))
    }

    private fun closeString(result: StringBuilder) = result.append("<!>")
    private fun openStartTag(result: StringBuilder) = result.append("<!")
    private fun closeStartTag(result: StringBuilder) = result.append("!>")
    private fun closeStartAndOpenNewTag(result: StringBuilder) = result.append("!><!")

    private fun checkOpenedAndCloseStringIfNeeded(opened: Stack<CodeMetaInfo>, end: Int, result: StringBuilder) {
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
