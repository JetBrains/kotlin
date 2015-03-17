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

package org.jetbrains.kotlin.jvm.compiler

import com.intellij.openapi.util.io.FileUtil
import org.jetbrains.kotlin.backend.common.output.OutputFile
import org.jetbrains.kotlin.load.kotlin.PackageClassUtils
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.JetFile
import org.jetbrains.kotlin.test.InTextDirectivesUtils
import org.jetbrains.kotlin.test.JetTestUtils
import org.jetbrains.org.objectweb.asm.ClassReader
import org.jetbrains.org.objectweb.asm.ClassVisitor
import org.jetbrains.org.objectweb.asm.Opcodes
import org.junit.Assert
import java.io.File

public trait AbstractSMAPBaseTest {

    private fun extractSMAPFromClasses(outputFiles: List<OutputFile>): List<SMAPAndFile> {
        return outputFiles.map { outputFile ->
            if (PackageClassUtils.isPackageClassFqName(FqName(FileUtil.getNameWithoutExtension(outputFile.relativePath).replace('/', '.')))) {
                // Don't test line numbers in *Package facade classes
                null
            }
            else {
                var debugInfo: String? = null
                ClassReader(outputFile.asByteArray()).accept(object : ClassVisitor(Opcodes.ASM5) {
                    override fun visitSource(source: String?, debug: String?) {
                        debugInfo = debug
                    }
                }, 0)

                SMAPAndFile.SMAPAndFile(debugInfo, outputFile.sourceFiles.single())
            }
        }.filterNotNull()
    }

    private fun extractSmapFromSource(file: JetFile): SMAPAndFile? {
        val fileContent = file.getText()
        val smapPrefix = "//SMAP"
        if (InTextDirectivesUtils.isDirectiveDefined(fileContent, smapPrefix)) {
            InTextDirectivesUtils.findLinesWithPrefixesRemoved(fileContent, smapPrefix)
            var smapData = fileContent.substring(fileContent.indexOf(smapPrefix))
            smapData = smapData.replaceAll("//", "").trim()

            return SMAPAndFile(if (smapData.startsWith("SMAP ABSENT")) null else smapData,
                               SMAPAndFile.getPath(file.getVirtualFile().getCanonicalPath()!!))
        }
        return null;
    }

    fun checkSMAP(inputFiles: List<JetFile>, outputFiles: List<OutputFile>) {
        val sourceData = inputFiles.map { extractSmapFromSource(it) }.filterNotNull()
        val compiledData = extractSMAPFromClasses(outputFiles).groupBy {
            it.sourceFile
        }.map {
            val smap = it.getValue().map { replaceHash(it.smap) }.filterNotNull().join("\n")
            SMAPAndFile(if (smap.isNotEmpty()) smap else null, it.key)
        }.toMap { it.sourceFile }

        for (source in sourceData) {
            val data = compiledData[source.sourceFile]
            Assert.assertEquals("Smap data differs for ${source.sourceFile}", source.smap, data?.smap?.trim())
        }
    }

    fun replaceHash(data: String?): String? {
        if (data == null) return null

        val fileSectionStart = data.indexOf("*F") + 3
        val lineSection = data.indexOf("*L") - 1

        val files = data.substring(fileSectionStart, lineSection).split("\n")

        val cleaned = files.map { JetTestUtils.replaceHashWithStar(it).replace("*", "HASH") }.join("\n")

        return data.substring(0, fileSectionStart) + cleaned + data.substring(lineSection)
    }

    class SMAPAndFile(val smap: String?, val sourceFile: String) {
        default object {
            fun SMAPAndFile(smap: String?, sourceFile: File) = SMAPAndFile(smap, getPath(sourceFile))

            public fun getPath(file: File): String {
                return getPath(file.getCanonicalPath())
            }

            public fun getPath(canonicalPath: String): String {
                //There are some problems with disk name on windows cause LightVirtualFile return it without disk name
                return FileUtil.toSystemIndependentName(canonicalPath).substringAfter(":")
            }
        }
    }
}