/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test

import org.jetbrains.kotlin.generators.dsl.TestGroup.TestClass
import org.jetbrains.kotlin.generators.dsl.junit5.generateTestGroupSuiteWithJUnit5
import org.jetbrains.kotlin.generators.util.TestGeneratorUtil
import org.jetbrains.kotlin.generators.util.TestGeneratorUtil.canFreezeIDE
import org.jetbrains.kotlin.spec.utils.tasks.detectDirsWithTestsMapFileOnly
import org.jetbrains.kotlin.test.runners.*
import org.jetbrains.kotlin.test.utils.CUSTOM_TEST_DATA_EXTENSION_PATTERN

fun main(args: Array<String>) {
    val mainClassName = TestGeneratorUtil.getMainClassName()
    val excludedCustomTestdataPattern = CUSTOM_TEST_DATA_EXTENSION_PATTERN
    val testRoot = args[0]

    generateTestGroupSuiteWithJUnit5(args, mainClassName) {
        testGroup(testRoot, testDataRoot = "compiler/testData") {
            testClass<AbstractFirLightTreeWithActualizerDiagnosticsWithLatestLanguageVersionTest>(suiteTestClassName = "FirOldFrontendMPPDiagnosticsWithLightTreeWithLatestLanguageVersionTestGenerated") {
                model("diagnostics/tests/multiplatform", pattern = "^(.*)\\.kts?$", excludedPattern = excludedCustomTestdataPattern)
            }

            run {
                val init: TestClass.() -> Unit = {
                    model(
                        "diagnostics/tests", pattern = TestGeneratorUtil.KT,
                        excludeDirsRecursively = listOf("multiplatform"),
                        excludedPattern = excludedCustomTestdataPattern,
                    )
                    model(
                        "diagnostics/testsWithStdLib",
                        excludedPattern = excludedCustomTestdataPattern,
                    )
                }

                testClass<AbstractFirLightTreeDiagnosticsWithLatestLanguageVersionTest>(
                    suiteTestClassName = "FirLightTreeOldFrontendDiagnosticsWithLatestLanguageVersionTestGenerated",
                    init = init
                )

                testClass<AbstractFirLightTreeDiagnosticsWithoutAliasExpansionTest>(
                    suiteTestClassName = "FirLightTreeOldFrontendDiagnosticsWithoutAliasExpansionTestGenerated",
                    init = init
                )
            }

            testClass<AbstractFirPsiForeignAnnotationsSourceJavaTest>(
                suiteTestClassName = "FirPsiOldFrontendForeignAnnotationsSourceJavaTestGenerated"
            ) {
                model("diagnostics/foreignAnnotationsTests/tests", excludedPattern = excludedCustomTestdataPattern)
                model("diagnostics/foreignAnnotationsTests/java8Tests", excludedPattern = excludedCustomTestdataPattern)
                model("diagnostics/foreignAnnotationsTests/java11Tests", excludedPattern = excludedCustomTestdataPattern)
            }

            testClass<AbstractFirPsiForeignAnnotationsCompiledJavaTest>(
                suiteTestClassName = "FirPsiOldFrontendForeignAnnotationsCompiledJavaTestGenerated"
            ) {
                model(
                    "diagnostics/foreignAnnotationsTests/tests",
                    excludedPattern = excludedCustomTestdataPattern,
                    excludeDirs = listOf("externalAnnotations"),
                )
                model("diagnostics/foreignAnnotationsTests/java8Tests", excludedPattern = excludedCustomTestdataPattern)
                model("diagnostics/foreignAnnotationsTests/java11Tests", excludedPattern = excludedCustomTestdataPattern)
            }

            testClass<AbstractFirPsiForeignAnnotationsCompiledJavaWithPsiClassReadingTest>(
                suiteTestClassName = "FirPsiOldFrontendForeignAnnotationsCompiledJavaWithPsiClassReadingTestGenerated"
            ) {
                model("diagnostics/foreignAnnotationsTests/tests", excludedPattern = excludedCustomTestdataPattern)
                model("diagnostics/foreignAnnotationsTests/java8Tests", excludedPattern = excludedCustomTestdataPattern)
                model("diagnostics/foreignAnnotationsTests/java11Tests", excludedPattern = excludedCustomTestdataPattern)
            }
        }

        testGroup(testRoot, "compiler/testData") {
            testClass<AbstractFirLoadK1CompiledJvmKotlinTest> {
                model("loadJava/compiledKotlin", extension = "kt")
                model("loadJava/compiledKotlinWithStdlib", extension = "kt")
            }

            testClass<AbstractFirLoadK2CompiledJvmKotlinTest> {
                model("loadJava/compiledKotlin", extension = "kt")
                model("loadJava/compiledKotlinWithStdlib", extension = "kt")
            }

            testClass<AbstractFirLoadCompiledJvmWithAnnotationsInMetadataKotlinTest> {
                model("loadJava/compiledKotlin", extension = "kt")
                model("loadJava/compiledKotlinWithStdlib", extension = "kt")
            }
        }

        testGroup(testRoot, "compiler/fir/analysis-tests/testData") {
            val init: TestClass.() -> Unit = {
                val relativeRootPaths = listOf(
                    "resolve",
                    "resolveWithStdlib",
                )

                for (path in relativeRootPaths) {
                    model(
                        path,
                        pattern = TestGeneratorUtil.KT_WITHOUT_DOTS_IN_NAME.canFreezeIDE,
                    )
                }
            }

            testClass<AbstractFirLightTreeDiagnosticsWithLatestLanguageVersionTest>(init = init)
            testClass<AbstractFirLightTreeDiagnosticsWithoutAliasExpansionTest>(init = init)

            testClass<AbstractMetadataDiagnosticTest> {
                model("metadataDiagnostic")
            }
        }

        testGroup(testRoot, "compiler/") {
            fun TestClass.phasedModel(allowKts: Boolean, excludeDirsRecursively: List<String> = emptyList()) {
                val relativeRootPaths = listOf(
                    "testData/diagnostics/tests",
                    "testData/diagnostics/testsWithAnyBackend",
                    "testData/diagnostics/testsWithStdLib",
                    "testData/diagnostics/jvmIntegration",
                    "fir/analysis-tests/testData/resolve",
                    "fir/analysis-tests/testData/resolveWithStdlib",
                )
                val pattern = when (allowKts) {
                    true -> TestGeneratorUtil.KT_OR_KTS
                    false -> TestGeneratorUtil.KT
                }

                for (path in relativeRootPaths) {
                    model(
                        path,
                        excludeDirs = listOf("declarations/multiplatform/k1"),
                        skipTestAllFilesCheck = true,
                        pattern = pattern.canFreezeIDE,
                        excludedPattern = CUSTOM_TEST_DATA_EXTENSION_PATTERN,
                        excludeDirsRecursively = excludeDirsRecursively,
                    )
                }
            }
            testClass<AbstractPhasedJvmDiagnosticLightTreeTest> {
                phasedModel(allowKts = false)
            }
            testClass<AbstractPhasedJvmDiagnosticPsiTest> {
                phasedModel(allowKts = true)
            }
        }

        testGroup(testRoot, testDataRoot = "compiler/tests-spec/testData") {
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
