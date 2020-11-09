/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.spec.utils.tasks

import org.jetbrains.kotlin.generators.tests.generator.generateTestGroupSuite
import org.jetbrains.kotlin.spec.checkers.AbstractDiagnosticsTestSpec
import org.jetbrains.kotlin.spec.checkers.AbstractFirDiagnosticsTestSpec
import org.jetbrains.kotlin.spec.codegen.AbstractBlackBoxCodegenTestSpec
import org.jetbrains.kotlin.spec.parsing.AbstractParsingTestSpec
import org.jetbrains.kotlin.spec.utils.GeneralConfiguration.SPEC_TESTDATA_PATH
import org.jetbrains.kotlin.spec.utils.GeneralConfiguration.SPEC_TEST_PATH
import org.jetbrains.kotlin.spec.utils.GeneralConfiguration.TESTS_MAP_FILENAME
import org.jetbrains.kotlin.spec.utils.SectionsJsonMapGenerator
import org.jetbrains.kotlin.spec.utils.TestsJsonMapGenerator
import java.io.File
import java.nio.file.Files

fun detectDirsWithTestsMapFileOnly(dirName: String): List<String> {
    val excludedDirs = mutableListOf<String>()

    File("$SPEC_TESTDATA_PATH/$dirName").walkTopDown().forEach { file ->
        val listFiles = Files.walk(file.toPath()).filter(Files::isRegularFile)

        if (file.isDirectory && listFiles?.allMatch { it.endsWith(TESTS_MAP_FILENAME) } == true) {
            val relativePath = file.relativeTo(File("$SPEC_TESTDATA_PATH/$dirName")).path

            if (!excludedDirs.any { relativePath.startsWith(it) }) {
                excludedDirs.add(relativePath)
            }
        }
    }

    return excludedDirs.sorted()
}

fun generateTests() {
    val excludedFirTestdataPattern = "^(.+)\\.fir\\.kts?\$"

    generateTestGroupSuite {
        testGroup(SPEC_TEST_PATH, SPEC_TESTDATA_PATH) {
            testClass<AbstractDiagnosticsTestSpec> {
                model(
                    "diagnostics",
                    excludeDirs = listOf("helpers") + detectDirsWithTestsMapFileOnly("diagnostics"),
                    excludedPattern = excludedFirTestdataPattern
                )
            }

            testClass<AbstractFirDiagnosticsTestSpec> {
                model(
                    "diagnostics",
                    excludeDirs = listOf("helpers") + detectDirsWithTestsMapFileOnly("diagnostics"),
                    excludedPattern = excludedFirTestdataPattern
                )
            }

            testClass<AbstractParsingTestSpec> {
                model(
                    relativeRootPath = "psi",
                    testMethod = "doParsingTest",
                    excludeDirs = listOf("helpers", "templates") + detectDirsWithTestsMapFileOnly("psi")
                )
            }
            testClass<AbstractBlackBoxCodegenTestSpec> {
                model("codegen/box", excludeDirs = listOf("helpers", "templates") + detectDirsWithTestsMapFileOnly("codegen/box"))
            }
        }
    }
}

fun main() {
    TestsJsonMapGenerator.buildTestsMapPerSection()
    SectionsJsonMapGenerator.writeSectionsMapJsons()
    generateTests()
}
