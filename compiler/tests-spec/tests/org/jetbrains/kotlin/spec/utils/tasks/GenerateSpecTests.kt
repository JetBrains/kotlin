/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.spec.utils.tasks

import org.jetbrains.kotlin.generators.generateTestGroupSuiteWithJUnit5
import org.jetbrains.kotlin.generators.impl.generateTestGroupSuite
import org.jetbrains.kotlin.spec.checkers.AbstractDiagnosticsTestSpec
import org.jetbrains.kotlin.spec.codegen.AbstractBlackBoxCodegenTestSpec
import org.jetbrains.kotlin.spec.parsing.AbstractParsingTestSpec
import org.jetbrains.kotlin.spec.utils.GeneralConfiguration.SPEC_TESTDATA_PATH
import org.jetbrains.kotlin.spec.utils.GeneralConfiguration.SPEC_TEST_PATH
import org.jetbrains.kotlin.spec.utils.GeneralConfiguration.TESTS_MAP_FILENAME
import org.jetbrains.kotlin.spec.utils.SectionsJsonMapGenerator
import org.jetbrains.kotlin.spec.utils.TestsJsonMapGenerator
import org.jetbrains.kotlin.test.runners.AbstractFirLightTreeDiagnosticTestSpec
import org.jetbrains.kotlin.test.runners.AbstractFirPsiDiagnosticTestSpec
import org.jetbrains.kotlin.test.utils.CUSTOM_TEST_DATA_EXTENSION_PATTERN
import java.io.File
import java.nio.file.Files

// `baseDir` is used in Kotlin plugin from IJ infra
fun detectDirsWithTestsMapFileOnly(dirName: String, baseDir: String = "."): List<String> {
    val excludedDirs = mutableListOf<String>()

    File("${baseDir}/$SPEC_TESTDATA_PATH/$dirName").walkTopDown().forEach { file ->
        val listFiles = Files.walk(file.toPath()).filter(Files::isRegularFile)

        if (file.isDirectory && listFiles?.allMatch { it.endsWith(TESTS_MAP_FILENAME) } == true) {
            val relativePath = file.relativeTo(File("${baseDir}/$SPEC_TESTDATA_PATH/$dirName")).path

            if (!excludedDirs.any { relativePath.startsWith(it) }) {
                excludedDirs.add(relativePath)
            }
        }
    }

    return excludedDirs.sorted().map { it.replace("\\", "/") }
}

fun generateTests() {
    generateTestGroupSuite {
        testGroup(SPEC_TEST_PATH, SPEC_TESTDATA_PATH) {
            testClass<AbstractDiagnosticsTestSpec> {
                model(
                    "diagnostics",
                    excludeDirs = listOf("helpers") + detectDirsWithTestsMapFileOnly("diagnostics"),
                    excludedPattern = CUSTOM_TEST_DATA_EXTENSION_PATTERN
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

    generateTestGroupSuiteWithJUnit5 {
        testGroup(testsRoot = "compiler/fir/analysis-tests/tests-gen", testDataRoot = SPEC_TESTDATA_PATH) {
            testClass<AbstractFirPsiDiagnosticTestSpec> {
                model(
                    "diagnostics",
                    excludeDirs = listOf("helpers") + detectDirsWithTestsMapFileOnly("diagnostics"),
                    excludedPattern = CUSTOM_TEST_DATA_EXTENSION_PATTERN
                )
            }
            testClass<AbstractFirLightTreeDiagnosticTestSpec> {
                model(
                    "diagnostics",
                    excludeDirs = listOf("helpers") + detectDirsWithTestsMapFileOnly("diagnostics"),
                    excludedPattern = CUSTOM_TEST_DATA_EXTENSION_PATTERN
                )
            }
        }
    }
}

fun main() {
    TestsJsonMapGenerator.buildTestsMapPerSection()
    SectionsJsonMapGenerator.writeSectionsMapJsons()
    generateTests()
}
