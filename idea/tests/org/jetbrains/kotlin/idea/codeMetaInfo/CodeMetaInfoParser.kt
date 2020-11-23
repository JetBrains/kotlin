/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.codeMetaInfo

import com.intellij.util.containers.Stack
import org.jetbrains.kotlin.idea.codeMetaInfo.models.ParsedCodeMetaInfo
import org.junit.Assert

object CodeMetaInfoParser {
    private val openingRegex = "(<!([^>]+?)!>)".toRegex()
    private val closingRegex = "(<!>)".toRegex()

    private val descriptionRegex = "\\(\".*?\"\\)".toRegex()
    private val platformRegex = "\\{(.+)}".toRegex()

    fun getCodeMetaInfoFromText(renderedText: String): List<ParsedCodeMetaInfo> {
        var text = renderedText
        val openingMatchResults = Stack<MatchResult>()
        val closingMatchResults = Stack<MatchResult>()
        val result = mutableListOf<ParsedCodeMetaInfo>()

        while (true) {
            var openingStartOffset = Int.MAX_VALUE
            var closingStartOffset = Int.MAX_VALUE
            val opening = openingRegex.find(text)
            val closing = closingRegex.find(text)
            if (opening == null && closing == null) break

            if (opening != null)
                openingStartOffset = opening.range.first
            if (closing != null)
                closingStartOffset = closing.range.first

            text = if (openingStartOffset < closingStartOffset) {
                openingMatchResults.push(opening)
                text.removeRange(openingStartOffset, opening!!.range.last + 1)
            } else {
                closingMatchResults.push(closing)
                text.removeRange(closingStartOffset, closing!!.range.last + 1)
            }
        }
        if (openingMatchResults.size != closingMatchResults.size) {
            Assert.fail("Opening and closing tags counts are not equals")
        }
        while (!openingMatchResults.isEmpty()) {
            val openingMatchResult = openingMatchResults.pop()
            val closingMatchResult = closingMatchResults.pop()
            val metaInfoWithoutParams = openingMatchResult.groups[2]!!.value.replace(descriptionRegex, "")
            metaInfoWithoutParams.split(",").forEach {
                val tag = platformRegex.replace(it, "").trim()
                val platforms =
                    if (platformRegex.containsMatchIn(it)) platformRegex.find(it)!!.destructured.component1().split(";") else listOf()
                result.add(
                    ParsedCodeMetaInfo(
                        openingMatchResult.range.first, closingMatchResult.range.first, platforms.toMutableList(), tag,
                    )
                )
            }
        }
        return result
    }
}