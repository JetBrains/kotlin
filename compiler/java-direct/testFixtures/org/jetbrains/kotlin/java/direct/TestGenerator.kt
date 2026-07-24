/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.java.direct

import org.jetbrains.kotlin.generators.dsl.junit5.generateTestGroupSuiteWithJUnit5
import org.jetbrains.kotlin.generators.util.TestGeneratorUtil
import org.jetbrains.kotlin.test.utils.CUSTOM_TEST_DATA_EXTENSION_PATTERN

fun main(args: Array<String>) {
    val baseGenPath = args[0]
    generateTestGroupSuiteWithJUnit5(args) {
        testGroup(baseGenPath, "compiler") {
            testClass<AbstractJavaUsingAstTest>("JavaUsingAstPhasedTestGenerated") {
                listOf(
                    "testData/diagnostics/tests",
                    "testData/diagnostics/testsWithAnyBackend",
                    "testData/diagnostics/testsWithStdLib",
                ).forEach { testDataRoot ->
                    model(
                        testDataRoot,
                        skipTestAllFilesCheck = true,
                        pattern = TestGeneratorUtil.KT,
                        excludedPattern = CUSTOM_TEST_DATA_EXTENSION_PATTERN,
                    )
                }
            }

            val excludedScriptDirs = listOf("script")

            testClass<AbstractJavaUsingAstBoxTest>("JavaUsingAstBoxTestGenerated") {
                model(
                    "testData/codegen/box",
                    excludeDirs = excludedScriptDirs,
                )
                model(
                    "testData/codegen/boxJvm",
                    excludeDirs = excludedScriptDirs,
                )
            }
        }
    }
}
