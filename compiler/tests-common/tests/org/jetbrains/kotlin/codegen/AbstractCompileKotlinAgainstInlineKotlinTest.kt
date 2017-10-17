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

import java.io.File

abstract class AbstractCompileKotlinAgainstInlineKotlinTest : AbstractCompileKotlinAgainstKotlinTest() {
    override fun doMultiFileTest(wholeFile: File, files: List<TestFile>, javaFilesDir: File?) {
        val (factory1, factory2) = doTwoFileTest(files.filter { it.name.endsWith(".kt") })
        try {
            val allGeneratedFiles = factory1.asList() + factory2.asList()
            val sourceFiles = factory1.inputFiles + factory2.inputFiles
            InlineTestUtil.checkNoCallsToInline(allGeneratedFiles.filterClassFiles(), sourceFiles)
            SMAPTestUtil.checkSMAP(files, allGeneratedFiles.filterClassFiles())
        }
        catch (e: Throwable) {
            println("FIRST:\n\n${factory1.createText()}\n\nSECOND:\n\n${factory2.createText()}")
            throw e
        }
    }
}
