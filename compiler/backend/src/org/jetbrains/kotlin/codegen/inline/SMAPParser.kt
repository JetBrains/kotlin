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

package org.jetbrains.kotlin.codegen.inline

object SMAPParser {
    /*null smap means that there is no any debug info in file (e.g. sourceName)*/
    @JvmStatic
    fun parseOrCreateDefault(mappingInfo: String?, source: String?, path: String, methodStartLine: Int, methodEndLine: Int): SMAP {
        if (mappingInfo != null && mappingInfo.isNotEmpty()) {
            parseOrNull(mappingInfo)?.let { return it }
        }
        if (source == null || source.isEmpty() || methodStartLine > methodEndLine) {
            return SMAP(listOf())
        }
        val mapping = FileMapping(source, path).apply {
            mapNewInterval(methodStartLine, methodStartLine, methodEndLine - methodStartLine + 1)
        }
        return SMAP(listOf(mapping))
    }

    fun parseOrNull(mappingInfo: String): SMAP? =
        parseStratum(mappingInfo, KOTLIN_STRATA_NAME, parseStratum(mappingInfo, KOTLIN_DEBUG_STRATA_NAME, null))

    private class SMAPTokenizer(private val text: String, private val headerString: String) : Iterator<String> {

        private var pos = 0
        private var currentLine: String? = null

        init {
            advance()
            while (currentLine != null && currentLine != headerString) {
                advance()
            }
            if (currentLine == headerString) {
                advance()
            }
        }

        private fun advance() {
            if (pos >= text.length) {
                currentLine = null
                return
            }
            val fromPos = pos
            while (pos < text.length && text[pos] != '\n' && text[pos] != '\r') pos++
            currentLine = text.substring(fromPos, pos)
            pos++
        }

        override fun hasNext(): Boolean {
            return currentLine != null
        }

        override fun next(): String {
            val res = currentLine ?: throw NoSuchElementException()
            advance()
            return res
        }
    }

    private fun parseStratum(mappingInfo: String, stratum: String, callSites: SMAP?): SMAP? {
        val fileMappings = linkedMapOf<Int, FileMapping>()
        val iterator = SMAPTokenizer(mappingInfo, "${SMAP.STRATA_SECTION} $stratum")
        // JSR-045 allows the line section to come before the file section, but we don't generate SMAPs like this.
        if (!iterator.hasNext() || iterator.next() != SMAP.FILE_SECTION) return null

        for (line in iterator) {
            when {
                line == SMAP.LINE_SECTION -> break
                line == SMAP.FILE_SECTION || line == SMAP.END || line.startsWith(SMAP.STRATA_SECTION) -> return null
            }

            val indexAndFileInternalName = if (line.startsWith("+ ")) line.substring(2) else line
            val fileIndex = indexAndFileInternalName.substringBefore(' ').toInt()
            val fileName = indexAndFileInternalName.substringAfter(' ')
            val path = if (line.startsWith("+ ")) iterator.next() else fileName
            fileMappings[fileIndex] = FileMapping(fileName, path)
        }

        for (line in iterator) {
            when {
                line == SMAP.LINE_SECTION || line == SMAP.FILE_SECTION -> return null
                line == SMAP.END || line.startsWith(SMAP.STRATA_SECTION) -> break
            }

            // <source>#<file>,<sourceRange>:<dest>,<destMultiplier>
            val fileSeparator = line.indexOf('#')
            if (fileSeparator < 0) return null
            val destSeparator = line.indexOf(':', fileSeparator)
            if (destSeparator < 0) return null
            val sourceRangeSeparator = line.indexOf(',').let { if (it !in fileSeparator..destSeparator) destSeparator else it }
            val destMultiplierSeparator = line.indexOf(',', destSeparator).let { if (it < 0) line.length else it }

            val file = fileMappings[line.substring(fileSeparator + 1, sourceRangeSeparator).toInt()] ?: return null
            val source = line.substring(0, fileSeparator).toInt()
            val dest = line.substring(destSeparator + 1, destMultiplierSeparator).toInt()
            val range = when {
                // These two fields have a different meaning, but for compatibility we treat them the same. See `SMAPBuilder`.
                destMultiplierSeparator != line.length -> line.substring(destMultiplierSeparator + 1).toInt()
                sourceRangeSeparator != destSeparator -> line.substring(sourceRangeSeparator + 1, destSeparator).toInt()
                else -> 1
            }
            // Here we assume that each range in `Kotlin` is entirely within at most one range in `KotlinDebug`.
            file.mapNewInterval(source, dest, range, callSites?.findRange(dest)?.let { it.mapDestToSource(it.dest) })
        }

        return SMAP(fileMappings.values.toList())
    }
}
