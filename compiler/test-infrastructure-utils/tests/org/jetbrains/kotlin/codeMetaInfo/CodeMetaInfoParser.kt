/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.codeMetaInfo

import org.jetbrains.kotlin.codeMetaInfo.model.ParsedCodeMetaInfo

object CodeMetaInfoParser {
    val openingRegex = """(<!([^"]*?((".*?")(, ".*?")*?)?[^"]*?)!>)""".toRegex()
    val closingRegex = """(<!>)""".toRegex()

    val openingOrClosingRegex = """(${closingRegex.pattern}|${openingRegex.pattern})""".toRegex()

    /*
     * ([\S&&[^,(){}]]+) -- tag, allowing all non-space characters except bracers and curly bracers
     * ([{](.*?)[}])? -- list of attributes
     * (\("((?:\\"|.)*?)"\))? -- arguments of meta info
     * (, )? -- possible separator between different infos
     * 
     * Note about escaping quotes in arguments:
     * `".*?"` matches everything between `"` and the closest next `"` that follows after. `\\"`
     * enforces that escaped `"` are matched "along with" other symbols matched via `.`, so that
     * the closing quote no longer has a change to match `\\"`.
     * Note that just using `.*` would match `<!TAG("A"), RAG("B")!>` as `A"), RAG("B`.
     */
    private val tagRegex = """([\S&&[^,(){}]]+)([{](.*?)[}])?(\("((?:\\"|.)*?)"\))?(, )?""".toRegex()

    private class Opening(val index: Int, val tags: String, val startOffset: Int) {
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

    fun getCodeMetaInfoFromText(renderedText: String): List<ParsedCodeMetaInfo> {
        var text = renderedText

        val openings = ArrayDeque<Opening>()
        val stackOfOpenings = ArrayDeque<Opening>()
        val closingOffsets = mutableMapOf<Opening, Int>()
        val result = mutableListOf<ParsedCodeMetaInfo>()

        var counter = 0

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
                requireNotNull(opening)
                val openingMatch = Opening(counter++, opening.groups[2]!!.value, opening.range.first)
                openings.addLast(openingMatch)
                stackOfOpenings.addLast(openingMatch)
                text.removeRange(openingStartOffset, opening.range.last + 1)
            } else {
                requireNotNull(closing)
                closingOffsets[stackOfOpenings.removeLast()] = closing.range.first
                text.removeRange(closingStartOffset, closing.range.last + 1)
            }
        }
        if (openings.size != closingOffsets.size) {
            error("Opening and closing tags counts are not equals")
        }
        while (!openings.isEmpty()) {
            val openingMatchResult = openings.removeLast()
            val closingMatchResult = closingOffsets.getValue(openingMatchResult)
            val allMetaInfos = openingMatchResult.tags
            tagRegex.findAll(allMetaInfos).map { it.groups }.forEach {
                val tag = it[1]!!.value
                val attributes = it[3]?.value?.split(";") ?: emptyList()
                val description = it[5]?.value

                result.add(
                    ParsedCodeMetaInfo(
                        openingMatchResult.startOffset,
                        closingMatchResult,
                        attributes.toMutableList(),
                        tag,
                        description
                    )
                )
            }
        }
        return result
    }
}
