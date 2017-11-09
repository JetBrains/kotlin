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

package org.jetbrains.kotlin.codegen

import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.util.text.StringUtil
import org.jetbrains.kotlin.backend.common.output.OutputFile
import org.jetbrains.kotlin.codegen.inline.GENERATE_SMAP
import org.jetbrains.kotlin.codegen.inline.RangeMapping
import org.jetbrains.kotlin.codegen.inline.SMAPParser
import org.jetbrains.kotlin.codegen.inline.toRange
import org.jetbrains.kotlin.utils.keysToMap
import org.jetbrains.org.objectweb.asm.ClassReader
import org.jetbrains.org.objectweb.asm.ClassVisitor
import org.jetbrains.org.objectweb.asm.Opcodes
import org.junit.Assert
import java.io.File
import java.io.StringReader

object SMAPTestUtil {
    private fun extractSMAPFromClasses(outputFiles: Iterable<OutputFile>): List<SMAPAndFile> {
        return outputFiles.mapNotNull { outputFile ->
            var debugInfo: String? = null
            ClassReader(outputFile.asByteArray()).accept(object : ClassVisitor(Opcodes.ASM5) {
                override fun visitSource(source: String?, debug: String?) {
                    debugInfo = debug
                }
            }, 0)

            SMAPAndFile.SMAPAndFile(debugInfo, outputFile.sourceFiles.single(), outputFile.relativePath)
        }
    }

    private fun extractSmapFromTestDataFile(file: CodegenTestCase.TestFile): SMAPAndFile? {
        if (!file.name.endsWith(".smap")) return null

        val content = buildString {
            StringReader(file.content).forEachLine { line ->
                // Strip comments
                if (!line.startsWith("//")) {
                    appendln(line.trim())
                }
            }
        }.trim()

        return SMAPAndFile(if (content.isNotEmpty()) content else null, SMAPAndFile.getPath(file.name), "NOT_SORTED")
    }

    fun checkSMAP(inputFiles: List<CodegenTestCase.TestFile>, outputFiles: Iterable<OutputFile>) {
        if (!GENERATE_SMAP) return

        val sourceData = inputFiles.mapNotNull { extractSmapFromTestDataFile(it) }
        val compiledSmaps = extractSMAPFromClasses(outputFiles)
        val compiledData = compiledSmaps.groupBy {
            it.sourceFile
        }.map {
            val smap = it.value.sortedByDescending { it.outputFile }.mapNotNull { it.smap }.joinToString("\n")
            SMAPAndFile(if (smap.isNotEmpty()) smap else null, it.key, "NOT_SORTED")
        }.associateBy { it.sourceFile }

        for (source in sourceData) {
            val ktFileName = "/" + source.sourceFile.replace(".smap", ".kt")
            val data = compiledData[ktFileName]
            Assert.assertEquals("Smap data differs for $ktFileName", normalize(source.smap), normalize(data?.smap))
        }

        checkNoConflictMappings(compiledSmaps)
    }

    private fun checkNoConflictMappings(compiledSmap: List<SMAPAndFile>?) {
        if (compiledSmap == null) return

        compiledSmap.mapNotNull { it.smap }.forEach {
            val smap = SMAPParser.parse(it)
            val conflictingLines = smap.fileMappings.flatMap {
                fileMapping ->
                fileMapping.lineMappings.flatMap {
                    lineMapping: RangeMapping ->
                    lineMapping.toRange.keysToMap { lineMapping }.entries
                }
            }.groupBy { it.key }.entries.filter { it.value.size != 1 }


            Assert.assertTrue(
                    conflictingLines.joinToString(separator = "\n") {
                        "Conflicting mapping for line ${it.key} in ${it.value.joinToString { it.toString() }} "
                    },
                    conflictingLines.isEmpty()
            )
        }
    }

    private fun normalize(text: String?) =
            text?.let { StringUtil.convertLineSeparators(it.trim()) }

    private class SMAPAndFile(val smap: String?, val sourceFile: String, val outputFile: String) {
        companion object {
            fun SMAPAndFile(smap: String?, sourceFile: File, outputFile: String) =
                    SMAPAndFile(smap, getPath(sourceFile), outputFile)

            fun getPath(file: File): String {
                return getPath(file.canonicalPath)
            }

            fun getPath(canonicalPath: String): String {
                //There are some problems with disk name on windows cause LightVirtualFile return it without disk name
                return FileUtil.toSystemIndependentName(canonicalPath).substringAfter(":")
            }
        }
    }
}
