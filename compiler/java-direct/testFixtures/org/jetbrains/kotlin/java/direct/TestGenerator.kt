/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.java.direct

import org.jetbrains.kotlin.generators.dsl.junit5.generateTestGroupSuiteWithJUnit5
import org.jetbrains.kotlin.generators.util.TestGeneratorUtil
import org.jetbrains.kotlin.test.utils.CUSTOM_TEST_DATA_EXTENSION_PATTERN
import java.io.File

fun main(args: Array<String>) {
    val baseGenPath = args[0]
    generateTestGroupSuiteWithJUnit5(args) {
        testGroup(baseGenPath, "compiler") {
            val javaFileRegex = Regex("^\\s*//\\s* FILE:\\s* .*\\.java\\s*\$")
            val additionalFileFilter: (File) -> Boolean =
                { file -> file.useLines { lines -> lines.any { it.matches(javaFileRegex) } } }
            testClass<AbstractJavaUsingAstTest>("JavaUsingAstPhasedTestGenerated") {
                listOf(
                    "testData/diagnostics/tests",
                    "testData/diagnostics/testsWithAnyBackend",
                    "testData/diagnostics/testsWithStdLib",
                    "testData/diagnostics/jvmIntegration",
                    "fir/analysis-tests/testData/resolve",
                    "fir/analysis-tests/testData/resolveWithStdlib",
                ).forEach { testDataRoot ->
                    model(
                        testDataRoot,
                        excludeDirs = listOf("declarations/multiplatform/k1"),
                        skipTestAllFilesCheck = true,
                        pattern = TestGeneratorUtil.KT,
                        excludedPattern = CUSTOM_TEST_DATA_EXTENSION_PATTERN,
                        additionalFileFilter = additionalFileFilter,
                    )
                }
            }

            val k1BoxTestDir = listOf("multiplatform/k1")
            val excludedScriptDirs = listOf("script")

            testClass<AbstractJavaUsingAstBoxTest>("JavaUsingAstBoxTestGenerated") {
                model(
                    "testData/codegen/box",
                    excludeDirs = k1BoxTestDir + excludedScriptDirs,
                    additionalFileFilter = additionalFileFilter,
                )
                model(
                    "testData/codegen/boxJvm",
                    excludeDirs = k1BoxTestDir + excludedScriptDirs,
                    additionalFileFilter = additionalFileFilter,
                )
            }
        }
    }
}
