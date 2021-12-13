/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.backend.handlers

import org.jetbrains.kotlin.backend.common.phaser.BeforeOrAfter
import org.jetbrains.kotlin.test.directives.CodegenTestDirectives
import org.jetbrains.kotlin.test.model.BinaryArtifacts
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.getOrCreateTempDirectory
import java.io.File

class PhasedIrDumpHandler(testServices: TestServices) : JvmBinaryArtifactHandler(testServices) {
    override fun processModule(module: TestModule, info: BinaryArtifacts.Jvm) {
        if (CodegenTestDirectives.DUMP_IR_FOR_GIVEN_PHASES !in module.directives) return
        val dumpDirectory = testServices.getOrCreateTempDirectory(DUMPED_IR_FOLDER_NAME)
        val dumpFiles = dumpDirectory.resolve(module.name).listFiles()?.filter { it.name.contains(AFTER_PREFIX) } ?: return
        val testFile = module.files.first()
        val testDirectory = testFile.originalFile.parentFile
        val visitedFiles = mutableListOf<String>()
        for (actualFile in dumpFiles) {
            val expectedFileName = testFile.originalFile.nameWithoutExtension + actualFile.name.removeRange(0, 2)
            visitedFiles += expectedFileName
            assertions.assertEqualsToFile(testDirectory.resolve(expectedFileName), actualFile.readText())
        }

        // check that all expected files has their actual counterpart
        val remainFiles = testDirectory
            .listFiles { _, name -> name.startsWith("${testFile.originalFile.nameWithoutExtension}_") }
            ?.filter { it.name !in visitedFiles } ?: return
        assertions.assertTrue(remainFiles.isEmpty()) {
            "There are some files in test directory (${remainFiles.joinToString { it.name }}) that don't have actual dump"
        }
    }

    override fun processAfterAllModules(someAssertionWasFailed: Boolean) {}

    companion object {
        const val DUMPED_IR_FOLDER_NAME = "dumped_ir"
        val AFTER_PREFIX = BeforeOrAfter.AFTER.name
        val BEFORE_PREFIX = BeforeOrAfter.BEFORE.name
    }
}
