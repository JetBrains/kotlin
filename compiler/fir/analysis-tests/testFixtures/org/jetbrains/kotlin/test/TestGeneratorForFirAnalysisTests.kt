/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test

import org.jetbrains.kotlin.generators.dsl.testClassPerDirectory
import org.jetbrains.kotlin.generators.dsl.junit5.generateTestGroupSuiteWithJUnit5
import org.jetbrains.kotlin.generators.util.TestGeneratorUtil
import org.jetbrains.kotlin.generators.util.TestGeneratorUtil.canFreezeIDE
import org.jetbrains.kotlin.test.runners.AbstractPhasedJvmDiagnosticLightTreeTest
import org.jetbrains.kotlin.test.utils.CUSTOM_TEST_DATA_EXTENSION_PATTERN

/**
 * Package of the generated test classes. Mirrored by the `testDataShards` declaration in
 * `compiler/fir/analysis-tests/build.gradle.kts`, which derives one test task per generated class.
 */
private const val GENERATED_TESTS_PACKAGE = "org.jetbrains.kotlin.test.runners.generated"

fun main(args: Array<String>) {
    val mainClassName = TestGeneratorUtil.getMainClassName()
    val testRoot = args[0]

    generateTestGroupSuiteWithJUnit5(args, mainClassName) {
        testGroup(testRoot, "compiler/") {
            val relativeRootPaths = listOf(
                "testData/diagnostics/tests",
                "testData/diagnostics/testsWithAnyBackend",
                "testData/diagnostics/testsWithStdLib",
            )

            for (path in relativeRootPaths) {
                testClassPerDirectory(
                    AbstractPhasedJvmDiagnosticLightTreeTest::class.java,
                    relativeRootPath = path,
                    generatedPackage = GENERATED_TESTS_PACKAGE,
                    excludeDirs = listOf("declarations/multiplatform/k1"),
                    skipTestAllFilesCheck = true,
                    pattern = TestGeneratorUtil.KT.canFreezeIDE,
                    excludedPattern = CUSTOM_TEST_DATA_EXTENSION_PATTERN,
                )
            }
        }
    }
}
