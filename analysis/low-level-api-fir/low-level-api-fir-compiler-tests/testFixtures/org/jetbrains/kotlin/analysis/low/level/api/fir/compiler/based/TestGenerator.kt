/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.compiler.based

import org.jetbrains.kotlin.analysis.low.level.api.fir.AbstractFirOutOfContentRootLazyBodiesCalculatorTest
import org.jetbrains.kotlin.analysis.low.level.api.fir.AbstractLLSourceAnnotationArgumentsCalculatorTest
import org.jetbrains.kotlin.analysis.low.level.api.fir.AbstractLLSourceLikeLazyBodiesCalculatorTest
import org.jetbrains.kotlin.analysis.low.level.api.fir.AbstractLLSourceLikeStubBasedResolutionTest
import org.jetbrains.kotlin.analysis.low.level.api.fir.diagnostic.compiler.based.*
import org.jetbrains.kotlin.generators.dsl.TestGroup
import org.jetbrains.kotlin.generators.dsl.junit5.generateTestGroupSuiteWithJUnit5
import org.jetbrains.kotlin.generators.util.TestGeneratorUtil
import org.jetbrains.kotlin.generators.util.TestGeneratorUtil.KT_OR_KTS
import org.jetbrains.kotlin.generators.util.TestGeneratorUtil.canFreezeIDE
import org.jetbrains.kotlin.spec.utils.GeneralConfiguration
import org.jetbrains.kotlin.spec.utils.tasks.detectDirsWithTestsMapFileOnly
import org.jetbrains.kotlin.test.utils.CUSTOM_TEST_DATA_EXTENSION_PATTERN

fun main(args: Array<String>) {
    val generatedTestRoot = args[0]
    generateTestGroupSuiteWithJUnit5(args) {
        testGroup(generatedTestRoot, "compiler/fir/raw-fir/psi2fir/testData") {
            testClass<AbstractLLSourceLikeLazyBodiesCalculatorTest> {
                model("rawBuilder", pattern = KT_OR_KTS)
            }

            testClass<AbstractFirOutOfContentRootLazyBodiesCalculatorTest> {
                model("rawBuilder", pattern = TestGeneratorUtil.KT)
            }

            testClass<AbstractLLSourceAnnotationArgumentsCalculatorTest> {
                model("rawBuilder", pattern = KT_OR_KTS)
            }

            testClass<AbstractLLSourceLikeStubBasedResolutionTest> {
                model("rawBuilder", pattern = KT_OR_KTS)
            }
        }

        testGroup(generatedTestRoot, "compiler/fir/analysis-tests/testData") {
            testClass<AbstractLLMetadataDiagnosticsTest> {
                model("metadataDiagnostic", pattern = TestGeneratorUtil.KT_WITHOUT_DOTS_IN_NAME.canFreezeIDE)
            }

            testClass<AbstractLLReversedMetadataDiagnosticsTest> {
                model("metadataDiagnostic", pattern = TestGeneratorUtil.KT_WITHOUT_DOTS_IN_NAME.canFreezeIDE)
            }
        }

        testGroup(generatedTestRoot, "plugins/scripting/scripting-tests/testData") {
            this.run {
                fun TestGroup.TestClass.scriptDiagnosticsInit() {
                    model(
                        "diagnostics/testScripts",
                        pattern = TestGeneratorUtil.KTS,
                        excludedPattern = CUSTOM_TEST_DATA_EXTENSION_PATTERN,
                    )
                }

                testClass<AbstractLLScriptWithCustomDefDiagnosticsTest> {
                    scriptDiagnosticsInit()
                }

                testClass<AbstractLLReversedScriptWithCustomDefDiagnosticsTest>() {
                    scriptDiagnosticsInit()
                }
            }

            run {
                fun TestGroup.TestClass.replDiagnosticsInit() {
                    model(
                        "diagnostics/repl",
                        pattern = TestGeneratorUtil.KTS,
                        excludedPattern = CUSTOM_TEST_DATA_EXTENSION_PATTERN,
                    )
                }

                testClass<AbstractLLReplDiagnosticsTest> {
                    replDiagnosticsInit()
                }

                testClass<AbstractLLReversedReplDiagnosticsTest> {
                    replDiagnosticsInit()
                }
            }


            this.run {
                fun TestGroup.TestClass.scriptCustomDefBackBoxInit() {
                    model(
                        "codegen/testScripts",
                        excludedPattern = CUSTOM_TEST_DATA_EXTENSION_PATTERN,
                        pattern = KT_OR_KTS,
                    )
                }

                testClass<AbstractLLScriptWithCustomDefBlackBoxTest> {
                    scriptCustomDefBackBoxInit()
                }

                testClass<AbstractLLReversedScriptWithCustomDefBlackBoxTest>() {
                    scriptCustomDefBackBoxInit()
                }
            }
        }

        testGroup(generatedTestRoot, "compiler/testData/diagnostics") {
            fun TestGroup.TestClass.modelInitWasmJs() {
                model("wasmTests", excludedPattern = CUSTOM_TEST_DATA_EXTENSION_PATTERN)
            }

            fun TestGroup.TestClass.modelInitWasmWasi() {
                model("wasmWasiTests", excludedPattern = CUSTOM_TEST_DATA_EXTENSION_PATTERN)
            }

            testClass<AbstractLLWasmJsDiagnosticsTest>(suiteTestClassName = "LLWasmJsDiagnosticsFe10TestGenerated") {
                modelInitWasmJs()
            }

            testClass<AbstractLLWasmWasiDiagnosticsTest>(suiteTestClassName = "LLWasmWasiDiagnosticsFe10TestGenerated") {
                modelInitWasmWasi()
            }

            testClass<AbstractLLReversedWasmJsDiagnosticsTest>(suiteTestClassName = "LLReversedWasmJsDiagnosticsFe10TestGenerated") {
                modelInitWasmJs()
            }

            testClass<AbstractLLReversedWasmWasiDiagnosticsTest>(suiteTestClassName = "LLReversedWasmWasiDiagnosticsFe10TestGenerated") {
                modelInitWasmWasi()
            }
        }

        testGroup(generatedTestRoot, "compiler/testData") {
            fun TestGroup.TestClass.modelInit() {
                model(
                    "diagnostics/testsWithJsStdLib",
                    excludedPattern = CUSTOM_TEST_DATA_EXTENSION_PATTERN,
                    pattern = KT_OR_KTS,
                )
            }

            testClass<AbstractLLJsDiagnosticsTest>(suiteTestClassName = "LLJsDiagnosticsFe10TestGenerated") {
                modelInit()
            }

            testClass<AbstractLLReversedJsDiagnosticsTest>(suiteTestClassName = "LLReversedJsDiagnosticsFe10TestGenerated") {
                modelInit()
            }
        }

        testGroup(generatedTestRoot, "compiler/testData") {
            fun TestGroup.TestClass.modelInit() {
                model(
                    "diagnostics/tests",
                    excludedPattern = CUSTOM_TEST_DATA_EXTENSION_PATTERN,
                    pattern = KT_OR_KTS.canFreezeIDE,
                    excludeDirs = listOf("jvm", "headerMode")
                )
                model(
                    "diagnostics/testsWithStdLib",
                    excludedPattern = CUSTOM_TEST_DATA_EXTENSION_PATTERN,
                    excludeDirs = listOf("native"),
                    pattern = KT_OR_KTS.canFreezeIDE,
                )
            }

            testClass<AbstractLLDiagnosticsTest> {
                modelInit()
            }

            testClass<AbstractLLReversedDiagnosticsTest> {
                modelInit()
            }

            testClass<AbstractLLPartialDiagnosticsTest> {
                modelInit()
            }

            testClass<AbstractLLBlackBoxTest> {
                model(
                    "codegen/box",
                    excludeDirs = listOf(
                        "script", // script is excluded until KT-60127 is implemented
                        "multiplatform/k1",
                    )
                )
            }

            testClass<AbstractLLReversedBlackBoxTest> {
                model(
                    "codegen/box",
                    excludeDirs = listOf(
                        "script", // script is excluded until KT-60127 is implemented
                        "multiplatform/k1",
                    )
                )
            }

            testClass<AbstractLLBlackBoxTest>(suiteTestClassName = "LLBlackBoxModernJdkTestGenerated") {
                model("codegen/boxModernJdk")
            }

            testClass<AbstractLLReversedBlackBoxTest>(suiteTestClassName = "LLReversedBlackBoxModernJdkTestGenerated") {
                model("codegen/boxModernJdk")
            }

            this.run {
                fun TestGroup.TestClass.scriptBlackBoxInit() {
                    model("codegen/script", pattern = TestGeneratorUtil.KTS)
                }

                testClass<AbstractLLScriptBlackBoxTest> {
                    scriptBlackBoxInit()
                }

                testClass<AbstractLLReversedScriptBlackBoxTest> {
                    scriptBlackBoxInit()
                }
            }
        }

        testGroup(
            testsRoot = generatedTestRoot,
            testDataRoot = GeneralConfiguration.SPEC_TESTDATA_PATH
        ) {
            fun TestGroup.TestClass.modelInit() {
                model(
                    "diagnostics",
                    excludeDirs = listOf("helpers") + detectDirsWithTestsMapFileOnly("diagnostics"),
                    excludedPattern = CUSTOM_TEST_DATA_EXTENSION_PATTERN,
                )
            }

            testClass<AbstractLLSpecTest> {
                modelInit()
            }

            testClass<AbstractLLReversedSpecTest> {
                modelInit()
            }
        }

        testGroup(
            testsRoot = generatedTestRoot,
            testDataRoot = "plugins/plugin-sandbox/testData"
        ) {
            testClass<AbstractLLSandboxBackBoxTest> {
                model("box", excludedPattern = CUSTOM_TEST_DATA_EXTENSION_PATTERN)
            }

            testClass<AbstractLLReversedSandboxBackBoxTest> {
                model("box", excludedPattern = CUSTOM_TEST_DATA_EXTENSION_PATTERN)
            }

            testClass<AbstractLLSandboxDiagnosticsTest> {
                model("diagnostics", excludedPattern = CUSTOM_TEST_DATA_EXTENSION_PATTERN)
            }

            testClass<AbstractLLReversedSandboxDiagnosticsTest> {
                model("diagnostics", excludedPattern = CUSTOM_TEST_DATA_EXTENSION_PATTERN)
            }
        }
    }
}
