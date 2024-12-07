/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.codeMetaInfo

import com.intellij.util.containers.Stack
import org.jetbrains.kotlin.codeMetaInfo.model.CodeMetaInfo
import java.io.File

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
        val sortedMetaInfos = getSortedCodeMetaInfos(codeMetaInfos).groupBy { it.start }
        val opened = Stack<CodeMetaInfo>()

        for ((i, c) in originalText.withIndex()) {
            processMetaInfosStartedAtOffset(i, sortedMetaInfos, opened, builder)
            builder.append(c)
        }
        val lastSymbolIsNewLine = builder.last() == '\n'
        if (lastSymbolIsNewLine) {
            builder.deleteCharAt(builder.length - 1)
        }
        processMetaInfosStartedAtOffset(originalText.length, sortedMetaInfos, opened, builder)
        if (lastSymbolIsNewLine) {
            builder.appendLine()
        }
    }

    private fun processMetaInfosStartedAtOffset(
        offset: Int,
        sortedMetaInfos: Map<Int, List<CodeMetaInfo>>,
        opened: Stack<CodeMetaInfo>,
        builder: StringBuilder
    ) {
        checkOpenedAndCloseStringIfNeeded(opened, offset, builder)
        val matchedCodeMetaInfos = sortedMetaInfos[offset] ?: emptyList()
        if (matchedCodeMetaInfos.isNotEmpty()) {
            val iterator = matchedCodeMetaInfos.listIterator()
            var current: CodeMetaInfo? = iterator.next()

            if (current != null) builder.append(current.tagPrefix)

            while (current != null) {
                val next: CodeMetaInfo? = if (iterator.hasNext()) iterator.next() else null
                val outer = opened.lastOrNull()
                if (outer != null) {
                    require(current.end <= outer.end) {
                        "The outer diagnostic ${outer.tag} at ${outer.start} ends at ${outer.end}, but the supposedly inner ${current?.tag} starting at ${current?.start} ends at ${current?.end}. Rendered so far:\n$builder"
                    }
                }
                opened.push(current)
                builder.append(current.asString())
                when {
                    next == null ->
                        builder.append(current.tagPostfix)
                    next.end == current.end ->
                        builder.append(", ")
                    else -> {
                        builder.append(current.tagPostfix)
                        builder.append(next.tagPrefix)
                    }
                }
                current = next
            }
        }
        // Here we need to handle meta infos which has start == end and close them immediately
        checkOpenedAndCloseStringIfNeeded(opened, offset, builder)
    }

    private val metaInfoComparator = (compareBy<CodeMetaInfo> { it.start } then compareByDescending { it.end }) then compareBy { it.tag }

    private fun getSortedCodeMetaInfos(metaInfos: Collection<CodeMetaInfo>): List<CodeMetaInfo> {
        return metaInfos.sortedWith(metaInfoComparator)
    }

    private fun checkOpenedAndCloseStringIfNeeded(opened: Stack<CodeMetaInfo>, end: Int, result: StringBuilder) {
        var prev: CodeMetaInfo? = null
        while (!opened.isEmpty() && end == opened.peek().end) {
            if (prev == null || prev.start != opened.peek().start)
                result.append(opened.peek().closingTag)
            prev = opened.pop()
        }
    }
}

fun clearFileFromDiagnosticMarkup(file: File) {
    val text = file.readText()
    val cleanText = clearTextFromDiagnosticMarkup(text)
    file.writeText(cleanText)
}

fun clearTextFromDiagnosticMarkup(text: String): String = text.replace(CodeMetaInfoParser.openingOrClosingRegex, "")
