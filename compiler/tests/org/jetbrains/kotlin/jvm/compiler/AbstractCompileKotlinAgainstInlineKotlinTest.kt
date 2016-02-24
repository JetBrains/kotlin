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

import org.jetbrains.kotlin.codegen.ClassFileFactory
import org.jetbrains.kotlin.codegen.CodegenTestCase
import org.jetbrains.kotlin.codegen.InlineTestUtil
import org.jetbrains.kotlin.codegen.filterClassFiles
import org.jetbrains.kotlin.load.kotlin.PackagePartClassUtils
import org.jetbrains.kotlin.test.KotlinTestUtils
import java.io.File

abstract class AbstractCompileKotlinAgainstInlineKotlinTest : AbstractCompileKotlinAgainstKotlinTest(), AbstractSMAPBaseTest {
    protected fun doBoxTest(firstFileName: String): Pair<ClassFileFactory, ClassFileFactory> {
        var files: List<CodegenTestCase.TestFile> = KotlinTestUtils.createTestFiles(
                firstFileName, KotlinTestUtils.doLoadFile(File(firstFileName)),
                object : KotlinTestUtils.TestFileFactory<Unit, CodegenTestCase.TestFile> {
                    override fun createFile(
                            module: Unit?, fileName: String, text: String, directives: Map<String, String>
                    ): CodegenTestCase.TestFile {
                        return CodegenTestCase.TestFile(fileName, text)
                    }

                    override fun createModule(name: String, dependencies: List<String>) {
                        throw UnsupportedOperationException()
                    }
                })

        // TODO: drop this (migrate codegen/box/inline/)
        if (files.size == 1) {
            val firstFile = files.iterator().next()
            val secondFile = File(firstFileName.replace("1.kt", "2.kt"))
            files = listOf(firstFile, CodegenTestCase.TestFile(secondFile.name, KotlinTestUtils.doLoadFile(secondFile)))
        }

        var factory1: ClassFileFactory? = null
        var factory2: ClassFileFactory? = null
        try {
            val fileA = files[1]
            val fileB = files[0]
            factory1 = compileA(fileA.name, fileA.content)
            factory2 = compileB(fileB.name, fileB.content)
            invokeBox(PackagePartClassUtils.getFilePartShortName(File(fileB.name).name))
        }
        catch (e: Throwable) {
            var result = ""
            if (factory1 != null) {
                result += "FIRST: \n\n" + factory1.createText()
            }
            if (factory2 != null) {
                result += "\n\nSECOND: \n\n" + factory2.createText()
            }
            println(result)
            throw e
        }

        return Pair(factory1, factory2)
    }

    fun doBoxTestWithInlineCheck(firstFileName: String) {
        val (factory1, factory2) = doBoxTest(firstFileName)
        val allGeneratedFiles = factory1.asList() + factory2.asList()

        try {
            val sourceFiles = factory1.inputFiles + factory2.inputFiles
            InlineTestUtil.checkNoCallsToInline(allGeneratedFiles.filterClassFiles(), sourceFiles)
            checkSMAP(sourceFiles, allGeneratedFiles.filterClassFiles())
        }
        catch (e: Throwable) {
            System.out.println(factory1.createText() + "\n" + factory2.createText())
            throw e
        }
    }
}
