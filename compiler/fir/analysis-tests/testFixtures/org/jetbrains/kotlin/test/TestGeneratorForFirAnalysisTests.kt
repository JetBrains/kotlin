/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test

import org.jetbrains.kotlin.generators.TestGroup.TestClass
import org.jetbrains.kotlin.generators.generateTestGroupSuiteWithJUnit5
import org.jetbrains.kotlin.generators.util.TestGeneratorUtil
import org.jetbrains.kotlin.generators.util.TestGeneratorUtil.canFreezeIDE
import org.jetbrains.kotlin.spec.utils.tasks.detectDirsWithTestsMapFileOnly
import org.jetbrains.kotlin.test.runners.AbstractFirLightTreeDiagnosticTestSpec
import org.jetbrains.kotlin.test.runners.AbstractFirLightTreeDiagnosticsWithLatestLanguageVersionTest
import org.jetbrains.kotlin.test.runners.AbstractFirLightTreeDiagnosticsWithoutAliasExpansionTest
import org.jetbrains.kotlin.test.runners.AbstractFirLightTreeWithActualizerDiagnosticsWithLatestLanguageVersionTest
import org.jetbrains.kotlin.test.runners.AbstractFirLoadCompiledJvmWithAnnotationsInMetadataKotlinTest
import org.jetbrains.kotlin.test.runners.AbstractFirLoadK1CompiledJvmKotlinTest
import org.jetbrains.kotlin.test.runners.AbstractFirLoadK2CompiledJvmKotlinTest
import org.jetbrains.kotlin.test.runners.AbstractFirPsiDiagnosticTestSpec
import org.jetbrains.kotlin.test.runners.AbstractFirPsiForeignAnnotationsCompiledJavaTest
import org.jetbrains.kotlin.test.runners.AbstractFirPsiForeignAnnotationsCompiledJavaWithPsiClassReadingTest
import org.jetbrains.kotlin.test.runners.AbstractFirPsiForeignAnnotationsSourceJavaTest
import org.jetbrains.kotlin.test.runners.AbstractPhasedJvmDiagnosticLightTreeTest
import org.jetbrains.kotlin.test.runners.AbstractPhasedJvmDiagnosticPsiTest
import org.jetbrains.kotlin.test.utils.CUSTOM_TEST_DATA_EXTENSION_PATTERN
import java.io.File

fun main(args: Array<String>) {
    val mainClassName = TestGeneratorUtil.getMainClassName()
    val excludedCustomTestdataPattern = CUSTOM_TEST_DATA_EXTENSION_PATTERN

    generateTestGroupSuiteWithJUnit5(args, mainClassName) {
        testGroup(testsRoot = "compiler/fir/analysis-tests/tests-gen", testDataRoot = "compiler/testData") {
            testClass<AbstractFirLightTreeWithActualizerDiagnosticsWithLatestLanguageVersionTest>(suiteTestClassName = "FirOldFrontendMPPDiagnosticsWithLightTreeWithLatestLanguageVersionTestGenerated") {
                model("diagnostics/tests/multiplatform", pattern = "^(.*)\\.kts?$", excludedPattern = excludedCustomTestdataPattern)
            }

            fun model(allowKts: Boolean, onlyTypealiases: Boolean = false): TestClass.() -> Unit = {
                val pattern = when (allowKts) {
                    true -> TestGeneratorUtil.KT_OR_KTS
                    false -> TestGeneratorUtil.KT
                }
                model(
                    "diagnostics/tests", pattern = pattern,
                    excludeDirsRecursively = listOf("multiplatform"),
                    excludedPattern = excludedCustomTestdataPattern,
                    skipSpecificFile = skipSpecificFileForFirDiagnosticTest(onlyTypealiases),
                    skipTestAllFilesCheck = onlyTypealiases
                )
                model(
                    "diagnostics/testsWithStdLib",
                    excludedPattern = excludedCustomTestdataPattern,
                    skipSpecificFile = skipSpecificFileForFirDiagnosticTest(onlyTypealiases),
                    skipTestAllFilesCheck = onlyTypealiases
                )
            }

            testClass<AbstractFirLightTreeDiagnosticsWithLatestLanguageVersionTest>(
                suiteTestClassName = "FirLightTreeOldFrontendDiagnosticsWithLatestLanguageVersionTestGenerated",
                init = model(allowKts = false)
            )

            testClass<AbstractFirLightTreeDiagnosticsWithoutAliasExpansionTest>(
                suiteTestClassName = "FirLightTreeOldFrontendDiagnosticsWithoutAliasExpansionTestGenerated",
                init = model(allowKts = false, onlyTypealiases = true)
            )

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

        testGroup("compiler/fir/analysis-tests/tests-gen", "compiler/testData") {
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

        testGroup("compiler/fir/analysis-tests/tests-gen", "compiler/fir/analysis-tests/testData") {
            fun model(allowKts: Boolean, onlyTypealiases: Boolean = false): TestClass.() -> Unit = {
                val relativeRootPaths = listOf(
                    "resolve",
                    "resolveWithStdlib",
                )
                val pattern = when (allowKts) {
                    true -> TestGeneratorUtil.KT_OR_KTS_WITHOUT_DOTS_IN_NAME
                    false -> TestGeneratorUtil.KT_WITHOUT_DOTS_IN_NAME
                }

                for (path in relativeRootPaths) {
                    model(
                        path,
                        pattern = pattern.canFreezeIDE,
                        skipSpecificFile = skipSpecificFileForFirDiagnosticTest(onlyTypealiases),
                        skipTestAllFilesCheck = onlyTypealiases
                    )
                }
            }

            testClass<AbstractFirLightTreeDiagnosticsWithLatestLanguageVersionTest>(init = model(allowKts = false))
            testClass<AbstractFirLightTreeDiagnosticsWithoutAliasExpansionTest>(init = model(allowKts = false, onlyTypealiases = true))
        }

        testGroup("compiler/fir/analysis-tests/tests-gen", "compiler/") {
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

        testGroup(testsRoot = "compiler/fir/analysis-tests/tests-gen", testDataRoot = "compiler/tests-spec/testData") {
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

private fun skipSpecificFileForFirDiagnosticTest(onlyTypealiases: Boolean): (File) -> Boolean {
    return when (onlyTypealiases) {
        true -> {
            { !it.readText().contains("typealias") }
        }
        false -> {
            { false }
        }
    }
}
