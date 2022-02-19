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

import com.intellij.openapi.util.text.StringUtil
import org.jetbrains.kotlin.backend.common.output.OutputFile
import org.jetbrains.kotlin.codegen.CommonSMAPTestUtil.SMAPAndFile
import org.jetbrains.kotlin.codegen.CommonSMAPTestUtil.checkNoConflictMappings
import org.jetbrains.kotlin.codegen.CommonSMAPTestUtil.extractSMAPFromClasses
import org.jetbrains.kotlin.codegen.inline.GENERATE_SMAP
import org.jetbrains.kotlin.test.KotlinBaseTest
import org.jetbrains.kotlin.test.util.JUnit4Assertions
import org.junit.Assert
import java.io.File
import java.io.StringReader

object SMAPTestUtil {
    private fun extractSmapFromTestDataFile(file: KotlinBaseTest.TestFile, separateCompilation: Boolean): SMAPAndFile? {
        if (!checkExtension(file, separateCompilation)) return null

        val content = buildString {
            StringReader(file.content).forEachLine { line ->
                // Strip comments
                if (!line.startsWith("//")) {
                    appendLine(line.trim())
                }
            }
        }.trim()

        return SMAPAndFile(if (content.isNotEmpty()) content else null, SMAPAndFile.getPath(file.name), "NOT_SORTED")
    }

    private fun checkExtension(file: KotlinBaseTest.TestFile, separateCompilation: Boolean) =
        file.name.run {
            endsWith(".smap") ||
                    if (separateCompilation) endsWith(".smap-separate-compilation") else endsWith(".smap-nonseparate-compilation")
        }

    fun checkSMAP(inputFiles: List<KotlinBaseTest.TestFile>, outputFiles: Iterable<OutputFile>, separateCompilation: Boolean) {
        if (!GENERATE_SMAP) return

        val sourceData = inputFiles.mapNotNull { extractSmapFromTestDataFile(it, separateCompilation) }
        val compiledSmaps = extractSMAPFromClasses(outputFiles)
        val compiledData = compiledSmaps.groupBy {
            it.sourceFile
        }.map {
            val smap = it.value.sortedByDescending(SMAPAndFile::outputFile).mapNotNull(SMAPAndFile::smap).joinToString("\n")
            SMAPAndFile(if (smap.isNotEmpty()) smap else null, it.key, "NOT_SORTED")
        }.associateBy { it.sourceFile }

        for (source in sourceData) {
            val ktFileName = "/" + File(source.sourceFile).name
                .replace(".smap-nonseparate-compilation", ".kt")
                .replace(".smap-separate-compilation", ".kt")
                .replace(".smap", ".kt")
            val data = compiledData[ktFileName]
            Assert.assertEquals("Smap data differs for $ktFileName", normalize(source.smap), normalize(data?.smap))
        }

        checkNoConflictMappings(compiledSmaps, JUnit4Assertions)
    }

    private fun normalize(text: String?) =
        text?.let { StringUtil.convertLineSeparators(it.trim()) }
}
