/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.codeMetaInfo

import org.jetbrains.kotlin.codeMetaInfo.model.ParsedCodeMetaInfo
import java.lang.StringBuilder

object CodeMetaInfoParser {
    private val openingRegex = """<!([^>][^"]*?((".*?")(, ".*?")*?)?[^"]*?)!>""".toRegex()
    private val closingRegex = "<!>".toRegex()
    private val commentRegex = """/\*(.*?)\*/""".toRegex(RegexOption.DOT_MATCHES_ALL)

    /*
     * ([\S&&[^,(){}]]+) -- tag, allowing all non-space characters except bracers and curly bracers
     * ([{](.*?)[}])? -- list of attributes
     * (\("(.*?)"\))? -- arguments of meta info
     * (, )? -- possible separator between different infos
     */
    private val tagRegex = """([\S&&[^,(){}]]+)([{](.*?)[}])?(\("(.*?)"\))?(, )?""".toRegex()

    private enum class MatchType {
        Opening,
        Closing,
    }

    private val matchTypeRegexMap = mutableMapOf(
        MatchType.Opening to openingRegex,
        MatchType.Closing to closingRegex
    )

    private abstract class MatchElement

    private class Opening(val index: Int, val tags: String, val startOffset: Int) : MatchElement() {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as Opening

            if (index != other.index) return false

            return true
        }

        override fun hashCode(): Int {
            return index
        }
    }

    private class Closing(val index: Int, val startOffset: Int) : MatchElement() {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as Closing

            if (index != other.index) return false

            return true
        }

        override fun hashCode(): Int {
            return index
        }
    }

    fun clearTextFromDiagnosticMarkup(text: String): String = getCodeMetaInfoAndClearedText(text).first

    fun getCodeMetaInfoFromText(text: String): List<ParsedCodeMetaInfo> = getCodeMetaInfoAndClearedText(text).second

    fun getCodeMetaInfoAndClearedText(text: String): Pair<String, List<ParsedCodeMetaInfo>> {
        val commentMatches = commentRegex.findAll(text).toList()

        val elements = mutableListOf<MatchElement>()

        val matches = mutableMapOf<MatchType, MatchResult?>()
        for (matchType in matchTypeRegexMap.keys) {
            matches[matchType] = matchTypeRegexMap[matchType]!!.find(text, 0)
        }

        val resultText = StringBuilder(text.length)
        var counter = 0
        var prevPosition = 0
        var selectedMatch: MatchResult?
        do {
            var selectedMatchType: MatchType = MatchType.Opening
            selectedMatch = null

            for (matchType in matches.keys) {
                val match = matches[matchType]
                if (match != null && match.range.first < (selectedMatch?.range?.first ?: Int.MAX_VALUE)) {
                    selectedMatch = match
                    selectedMatchType = matchType
                }
            }

            if (selectedMatch != null) {
                if (!commentMatches.any { commentMatch ->
                        selectedMatch.range.first >= commentMatch.range.first &&
                                selectedMatch.range.first <= commentMatch.range.last
                    }) {
                    resultText.append(text, prevPosition, selectedMatch.range.first)
                    prevPosition = selectedMatch.range.last + 1
                    elements.add(
                        if (selectedMatchType == MatchType.Opening) {
                            Opening(counter++, selectedMatch.groupValues[1], resultText.length)
                        } else {
                            Closing(counter++, resultText.length)
                        }
                    )
                }
                matches[selectedMatchType] = matchTypeRegexMap[selectedMatchType]!!.find(text, selectedMatch.range.last + 1)
            }
        } while (selectedMatch != null)

        resultText.append(text, prevPosition, text.length)

        val openings = ArrayDeque<Opening>()
        val result = mutableListOf<ParsedCodeMetaInfo>()

        for (element in elements) {
            if (element is Opening) {
                openings.addLast(element)
            } else if (element is Closing) {
                val opening = openings.removeLastOrNull() ?: error("Excess closing element")
                val allMetaInfos = opening.tags
                tagRegex.findAll(allMetaInfos).map { it.groups }.forEach {
                    val tag = it[1]!!.value
                    val attributes = it[3]?.value?.split(";") ?: emptyList()
                    val description = it[5]?.value

                    result.add(
                        ParsedCodeMetaInfo(
                            opening.startOffset,
                            element.startOffset,
                            attributes.toMutableList(),
                            tag,
                            description
                        )
                    )
                }
            }
        }

        if (openings.size > 0)
            error("Opening element does not have corresponding closing")

        return Pair(resultText.toString(), result)
    }
}
