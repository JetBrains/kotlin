/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.compiler.based

import org.jetbrains.kotlin.analysis.low.level.api.fir.AbstractFirOutOfContentRootLazyBodiesCalculatorTest
import org.jetbrains.kotlin.analysis.low.level.api.fir.AbstractFirScriptLazyBodiesCalculatorTest
import org.jetbrains.kotlin.analysis.low.level.api.fir.AbstractFirSourceLazyBodiesCalculatorTest
import org.jetbrains.kotlin.analysis.low.level.api.fir.AbstractLLScriptStubBasedResolutionTest
import org.jetbrains.kotlin.analysis.low.level.api.fir.AbstractLLSourceAnnotationArgumentsCalculatorTest
import org.jetbrains.kotlin.analysis.low.level.api.fir.AbstractLLSourceStubBasedResolutionTest
import org.jetbrains.kotlin.analysis.low.level.api.fir.diagnostic.compiler.based.AbstractLLBlackBoxTest
import org.jetbrains.kotlin.analysis.low.level.api.fir.diagnostic.compiler.based.AbstractLLDiagnosticsTest
import org.jetbrains.kotlin.analysis.low.level.api.fir.diagnostic.compiler.based.AbstractLLJsDiagnosticsTest
import org.jetbrains.kotlin.analysis.low.level.api.fir.diagnostic.compiler.based.AbstractLLPartialDiagnosticsTest
import org.jetbrains.kotlin.analysis.low.level.api.fir.diagnostic.compiler.based.AbstractLLReversedBlackBoxTest
import org.jetbrains.kotlin.analysis.low.level.api.fir.diagnostic.compiler.based.AbstractLLReversedDiagnosticsTest
import org.jetbrains.kotlin.analysis.low.level.api.fir.diagnostic.compiler.based.AbstractLLReversedJsDiagnosticsTest
import org.jetbrains.kotlin.analysis.low.level.api.fir.diagnostic.compiler.based.AbstractLLReversedSandboxBackBoxTest
import org.jetbrains.kotlin.analysis.low.level.api.fir.diagnostic.compiler.based.AbstractLLReversedSandboxDiagnosticsTest
import org.jetbrains.kotlin.analysis.low.level.api.fir.diagnostic.compiler.based.AbstractLLReversedScriptBlackBoxTest
import org.jetbrains.kotlin.analysis.low.level.api.fir.diagnostic.compiler.based.AbstractLLReversedScriptWithCustomDefBlackBoxTest
import org.jetbrains.kotlin.analysis.low.level.api.fir.diagnostic.compiler.based.AbstractLLReversedScriptWithCustomDefDiagnosticsTest
import org.jetbrains.kotlin.analysis.low.level.api.fir.diagnostic.compiler.based.AbstractLLReversedSpecTest
import org.jetbrains.kotlin.analysis.low.level.api.fir.diagnostic.compiler.based.AbstractLLReversedWasmJsDiagnosticsTest
import org.jetbrains.kotlin.analysis.low.level.api.fir.diagnostic.compiler.based.AbstractLLReversedWasmWasiDiagnosticsTest
import org.jetbrains.kotlin.analysis.low.level.api.fir.diagnostic.compiler.based.AbstractLLSandboxBackBoxTest
import org.jetbrains.kotlin.analysis.low.level.api.fir.diagnostic.compiler.based.AbstractLLSandboxDiagnosticsTest
import org.jetbrains.kotlin.analysis.low.level.api.fir.diagnostic.compiler.based.AbstractLLScriptBlackBoxTest
import org.jetbrains.kotlin.analysis.low.level.api.fir.diagnostic.compiler.based.AbstractLLScriptWithCustomDefBlackBoxTest
import org.jetbrains.kotlin.analysis.low.level.api.fir.diagnostic.compiler.based.AbstractLLScriptWithCustomDefDiagnosticsTest
import org.jetbrains.kotlin.analysis.low.level.api.fir.diagnostic.compiler.based.AbstractLLSpecTest
import org.jetbrains.kotlin.analysis.low.level.api.fir.diagnostic.compiler.based.AbstractLLWasmJsDiagnosticsTest
import org.jetbrains.kotlin.analysis.low.level.api.fir.diagnostic.compiler.based.AbstractLLWasmWasiDiagnosticsTest
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
            testClass<AbstractFirSourceLazyBodiesCalculatorTest> {
                model("rawBuilder", pattern = TestGeneratorUtil.KT)
            }

            testClass<AbstractFirOutOfContentRootLazyBodiesCalculatorTest> {
                model("rawBuilder", pattern = TestGeneratorUtil.KT)
            }

            testClass<AbstractFirScriptLazyBodiesCalculatorTest> {
                model("rawBuilder", pattern = TestGeneratorUtil.KTS)
            }

            testClass<AbstractLLSourceAnnotationArgumentsCalculatorTest> {
                model("rawBuilder", pattern = TestGeneratorUtil.KT)
            }

            testClass<AbstractLLSourceStubBasedResolutionTest> {
                model("rawBuilder", pattern = TestGeneratorUtil.KT)
            }

            testClass<AbstractLLScriptStubBasedResolutionTest> {
                model("rawBuilder", pattern = TestGeneratorUtil.KTS)
            }
        }

        testGroup(generatedTestRoot, "compiler/fir/analysis-tests/testData") {
            fun TestGroup.TestClass.modelInit() {
                model("resolve", pattern = TestGeneratorUtil.KT_WITHOUT_DOTS_IN_NAME.canFreezeIDE, excludeDirs = listOf("headerMode"))
                model("resolveWithStdlib", pattern = TestGeneratorUtil.KT_WITHOUT_DOTS_IN_NAME.canFreezeIDE)
            }

            testClass<AbstractLLDiagnosticsTest>(suiteTestClassName = "LLDiagnosticsFirTestGenerated") {
                modelInit()
            }

            testClass<AbstractLLReversedDiagnosticsTest>(suiteTestClassName = "LLReversedDiagnosticsFirTestGenerated") {
                modelInit()
            }

            testClass<AbstractLLPartialDiagnosticsTest>(suiteTestClassName = "LLPartialDiagnosticsFirTestGenerated") {
                modelInit()
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
                    pattern = KT_OR_KTS,
                )
                model(
                    "diagnostics/testsWithStdLib",
                    excludedPattern = CUSTOM_TEST_DATA_EXTENSION_PATTERN,
                    excludeDirs = listOf("native"),
                    pattern = KT_OR_KTS,
                )
            }

            testClass<AbstractLLDiagnosticsTest>(suiteTestClassName = "LLDiagnosticsFe10TestGenerated") {
                modelInit()
            }

            testClass<AbstractLLReversedDiagnosticsTest>(suiteTestClassName = "LLReversedDiagnosticsFe10TestGenerated") {
                modelInit()
            }

            testClass<AbstractLLPartialDiagnosticsTest>(suiteTestClassName = "LLPartialDiagnosticsFe10TestGenerated") {
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