/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.codegen

import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.JVMConfigurationKeys
import org.jetbrains.kotlin.test.KotlinTestUtils
import java.io.File

abstract class AbstractDumpDeclarationsTest : CodegenTestCase() {

    private lateinit var dumpToFile: File

    override fun doMultiFileTest(wholeFile: File, files: List<TestFile>) {
        val expectedResult = KotlinTestUtils.replaceExtension(wholeFile, "json")
        dumpToFile = KotlinTestUtils.tmpDirForTest(this).resolve("$name.json")
        compile(files)
        classFileFactory.generationState.destroy()
        KotlinTestUtils.assertEqualsToFile(expectedResult, dumpToFile.readText()) {
            it.replace("COROUTINES_PACKAGE", coroutinesPackage)
        }
    }

    override fun updateConfiguration(configuration: CompilerConfiguration) {
        super.updateConfiguration(configuration)
        configuration.put(JVMConfigurationKeys.DECLARATIONS_JSON_PATH, dumpToFile.path)
    }
}
