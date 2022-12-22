/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.backend.handlers

import org.jetbrains.kotlin.test.directives.CodegenTestDirectives
import org.jetbrains.kotlin.test.model.BinaryArtifacts
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.assertions
import org.jetbrains.kotlin.test.services.getOrCreateTempDirectory
import java.io.File

class IrInterpreterDumpHandler(testServices: TestServices) : JvmBinaryArtifactHandler(testServices) {
    private fun getDump(module: TestModule): Pair<File, File>? {
        val dumpDirectory = testServices.getOrCreateTempDirectory(PhasedIrDumpHandler.DUMPED_IR_FOLDER_NAME)
        val dumpFiles = dumpDirectory.resolve(module.name).listFiles()
        val before = dumpFiles?.single { it.name.contains(PhasedIrDumpHandler.BEFORE_PREFIX) } ?: return null
        val after = dumpFiles.single { it.name.contains(PhasedIrDumpHandler.AFTER_PREFIX) } ?: return null
        return Pair(before, after)
    }

    private fun findConstProperties(text: String): List<Int> {
        return text.split("\n")
            .mapIndexed { index: Int, line: String -> Pair(index, line) }
            .filter { it.second.trim().startsWith("PROPERTY") && it.second.contains("[const,val]") }
            .map { it.first }
    }

    private fun checkInitializersAreConst(text: String, propertiesStart: List<Int>) {
        val lines = text.split("\n")
        for (index in propertiesStart) {
            testServices.assertions.assertTrue(lines[index + 3].trim().startsWith("CONST")) {
                "Property at $index wasn't converted to const"
            }
        }
    }

    override fun processModule(module: TestModule, info: BinaryArtifacts.Jvm) {
        if (CodegenTestDirectives.DUMP_IR_FOR_GIVEN_PHASES !in module.directives) return
        val (before, after) = getDump(module) ?: testServices.assertions.fail { "Cannot find BEFORE and AFTER lowering files" }

        val beforeText = before.readText()
        val afterText = after.readText()
        val propertiesBefore = findConstProperties(beforeText)
        val propertiesAfter = findConstProperties(afterText)
        testServices.assertions.assertTrue(propertiesBefore.isNotEmpty()) { "Test file doesn't contain const properties" }
        testServices.assertions.assertEquals(propertiesBefore.size, propertiesAfter.size) {
            "After lowering some properties are missing"
        }

        checkInitializersAreConst(afterText, propertiesAfter)
    }

    override fun processAfterAllModules(someAssertionWasFailed: Boolean) {}
}
