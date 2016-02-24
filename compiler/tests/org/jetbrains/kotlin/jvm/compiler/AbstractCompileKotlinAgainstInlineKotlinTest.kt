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

import org.jetbrains.kotlin.checkers.KotlinMultiFileTestWithJava
import org.jetbrains.kotlin.codegen.ClassFileFactory
import org.jetbrains.kotlin.codegen.InlineTestUtil
import org.jetbrains.kotlin.codegen.filterClassFiles
import org.jetbrains.kotlin.load.kotlin.PackagePartClassUtils
import java.io.File

abstract class AbstractCompileKotlinAgainstInlineKotlinTest : AbstractCompileKotlinAgainstKotlinTest(), AbstractSMAPBaseTest {
    override fun doMultiFileTest(
            file: File, modules: Map<String, KotlinMultiFileTestWithJava<Void, TestFile>.ModuleAndDependencies>, files: List<TestFile>
    ) {
        val kotlinFiles = files.filter { it.name.endsWith(".kt") }
        assert(kotlinFiles.size == 2) { "There should be exactly two files in this test" }

        var factory1: ClassFileFactory? = null
        var factory2: ClassFileFactory? = null
        try {
            val (fileA, fileB) = kotlinFiles
            factory1 = compileA(fileA.name, fileA.content)
            factory2 = compileB(fileB.name, fileB.content)
            invokeBox(PackagePartClassUtils.getFilePartShortName(File(fileB.name).name))

            val allGeneratedFiles = factory1.asList() + factory2.asList()

            val sourceFiles = factory1.inputFiles + factory2.inputFiles
            InlineTestUtil.checkNoCallsToInline(allGeneratedFiles.filterClassFiles(), sourceFiles)
            checkSMAP(files, allGeneratedFiles.filterClassFiles())
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
    }
}
