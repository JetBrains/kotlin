/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.codeMetaInfo

import org.jetbrains.kotlin.codeMetaInfo.model.ParsedCodeMetaInfo
import org.jetbrains.kotlin.test.checkTestInfrastructure

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
        val openings = ArrayDeque<Opening>()
        val stackOfOpenings = ArrayDeque<Opening>()
        val closingOffsets = mutableMapOf<Opening, Int>()
        val result = mutableListOf<ParsedCodeMetaInfo>()

        var counter = 0

        // Offsets refer to the text without meta infos, so the length of the meta infos found before is subtracted.
        // A closing meta info is preferred to an opening one which starts at the same offset.
        var removedLength = 0
        var match = openingOrClosingRegex.find(renderedText)
        while (match != null) {
            val startOffset = match.range.first - removedLength
            if (match.groups[2] != null) {
                closingOffsets[stackOfOpenings.removeLast()] = startOffset
            } else {
                val openingMatch = Opening(counter++, match.groups[4]!!.value, startOffset)
                openings.addLast(openingMatch)
                stackOfOpenings.addLast(openingMatch)
            }

            removedLength += match.value.length
            match = match.next()
        }
        checkTestInfrastructure(openings.size == closingOffsets.size) {
            "Opening and closing tags counts are not equals"
        }
        while (!openings.isEmpty()) {
            val openingMatchResult = openings.removeLast()
            val closingMatchResult = closingOffsets.getValue(openingMatchResult)
            val allMetaInfos = openingMatchResult.tags
            tagRegex.findAll(allMetaInfos).map { it.groups }.forEach {
                val tag = it[1]!!.value
                val attributes = it[3]?.value?.split(";") ?: emptyList()
                val description = it[5]?.value

                val distinctAttributes = attributes.distinct()
                val duplications = attributes.size - distinctAttributes.size
                (0..duplications).mapTo(result) {
                    ParsedCodeMetaInfo(
                        openingMatchResult.startOffset,
                        closingMatchResult,
                        distinctAttributes.toMutableList(),
                        tag,
                        description
                    )
                }
            }
        }
        return result
    }
}
