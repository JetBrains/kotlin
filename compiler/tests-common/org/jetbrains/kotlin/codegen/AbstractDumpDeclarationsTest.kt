/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.JVMConfigurationKeys
import org.jetbrains.kotlin.test.ConfigurationKind
import org.jetbrains.kotlin.test.KotlinTestUtils
import java.io.File

abstract class AbstractDumpDeclarationsTest : CodegenTestCase() {

    private lateinit var dumpToFile: File

    override fun doMultiFileTest(wholeFile: File, files: List<TestFile>, javaFilesDir: File?) {
        val expectedResult = KotlinTestUtils.replaceExtension(wholeFile, "json")
        dumpToFile = KotlinTestUtils.tmpDirForTest(this).resolve(name + ".json")
        compile(files, null)
        classFileFactory.generationState.destroy()
        KotlinTestUtils.assertEqualsToFile(expectedResult, dumpToFile.readText())
    }

    override fun updateConfiguration(configuration: CompilerConfiguration) {
        configuration.put(JVMConfigurationKeys.DECLARATIONS_JSON_PATH, dumpToFile.path)
    }

    override fun extractConfigurationKind(files: MutableList<TestFile>): ConfigurationKind {
        return ConfigurationKind.NO_KOTLIN_REFLECT
    }
}
