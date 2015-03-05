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

class SMAPParser(val mappingInfo: String?, val source: String, val path: String, val methodStartLine: Int, val methodEndLine: Int) {

    val fileMappings = linkedMapOf<Int, FileMapping>()

    fun parse() : SMAP {
        if (mappingInfo == null || mappingInfo.isEmpty()) {
            val fm = FileMapping(source, path)
            if (methodStartLine <= methodEndLine) {
                //one to one
                fm.addRangeMapping(RangeMapping(methodStartLine, methodStartLine, methodEndLine - methodStartLine + 1))
            }
            return SMAP(listOf(fm))
        }

        val fileSectionStart = mappingInfo.indexOf(SMAP.FILE_SECTION) + SMAP.FILE_SECTION.length()
        val lineSectionAnchor = mappingInfo.indexOf(SMAP.LINE_SECTION)
        val files = mappingInfo.substring(fileSectionStart, lineSectionAnchor)


        val fileEntries = files.trim().split('+')

        for (fileDeclaration in fileEntries) {
            if (fileDeclaration == "") continue;
            val fileInternalName = fileDeclaration.trim()

            val indexEnd = fileInternalName.indexOf(' ')
            val fileIndex = Integer.valueOf(fileInternalName.substring(0, indexEnd))
            val newLine = fileInternalName.indexOf('\n')
            val fileName = fileInternalName.substring(indexEnd + 1, newLine)
            fileMappings.put(fileIndex, FileMapping(fileName, fileInternalName.substring(newLine + 1).trim()))
        }


        val lines = mappingInfo.substring(lineSectionAnchor + SMAP.LINE_SECTION.length(), mappingInfo.indexOf(SMAP.END)).trim().split('\n')
        for (lineMapping in lines) {
            /*only simple mapping now*/
            val targetSplit = lineMapping.indexOf(':')
            val originalPart = lineMapping.substring(0, targetSplit)
            var rangeSeparator = originalPart.indexOf(',').let { if (it < 0) targetSplit else it}

            val fileSeparator = lineMapping.indexOf('#')
            val originalIndex = Integer.valueOf(originalPart.substring(0, fileSeparator))
            val range = if (rangeSeparator == targetSplit) 1 else Integer.valueOf(originalPart.substring(rangeSeparator + 1, targetSplit))

            val fileIndex = Integer.valueOf(lineMapping.substring(fileSeparator + 1, rangeSeparator))
            val targetIndex = Integer.valueOf(lineMapping.substring(targetSplit + 1))
            fileMappings[fileIndex]!!.addRangeMapping(RangeMapping(originalIndex, targetIndex, range))
        }

        return SMAP(fileMappings.values().toList())
    }
}