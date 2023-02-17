/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.generators

import org.jetbrains.kotlin.generators.generateTestGroupSuiteWithJUnit5
import org.jetbrains.kotlin.generators.util.TestGeneratorUtil
import org.jetbrains.kotlin.test.TargetBackend
import org.jetbrains.kotlin.test.runners.*
import org.jetbrains.kotlin.test.runners.codegen.*
import org.jetbrains.kotlin.test.runners.ir.AbstractFir2IrTextTest
import org.jetbrains.kotlin.test.runners.ir.AbstractIrTextTest
import org.jetbrains.kotlin.test.runners.ir.AbstractFirPsi2IrTextTest
import org.jetbrains.kotlin.test.runners.ir.interpreter.AbstractJvmIrInterpreterAfterFir2IrTest
import org.jetbrains.kotlin.test.runners.ir.interpreter.AbstractJvmIrInterpreterAfterPsi2IrTest
import org.jetbrains.kotlin.test.utils.CUSTOM_TEST_DATA_EXTENSION_PATTERN
import org.jetbrains.kotlin.visualizer.fir.AbstractFirVisualizerTest
import org.jetbrains.kotlin.visualizer.psi.AbstractPsiVisualizerTest

fun generateJUnit5CompilerTests(args: Array<String>) {
    val excludedCustomTestdataPattern = CUSTOM_TEST_DATA_EXTENSION_PATTERN

    generateTestGroupSuiteWithJUnit5(args) {
        testGroup(testsRoot = "compiler/tests-common-new/tests-gen", testDataRoot = "compiler/testData") {
            testClass<AbstractDiagnosticTest> {
                model("diagnostics/tests", pattern = "^(.*)\\.kts?$", excludedPattern = excludedCustomTestdataPattern)
                model("diagnostics/testsWithStdLib", excludedPattern = excludedCustomTestdataPattern)
            }

            testClass<AbstractDiagnosticUsingJavacTest> {
                model("diagnostics/tests/javac", pattern = "^(.*)\\.kts?$", excludedPattern = excludedCustomTestdataPattern)
            }

            testClass<AbstractDiagnosticsTestWithJsStdLib> {
                model("diagnostics/testsWithJsStdLib", excludedPattern = excludedCustomTestdataPattern)
            }

            testClass<AbstractDiagnosticsTestWithOldJvmBackend> {
                model(
                    "diagnostics/testsWithJvmBackend",
                    targetBackend = TargetBackend.JVM_OLD,
                    excludedPattern = excludedCustomTestdataPattern
                )
            }

            testClass<AbstractDiagnosticsTestWithJvmIrBackend> {
                model(
                    "diagnostics/testsWithJvmBackend",
                    pattern = "^(.+)\\.kts?$",
                    targetBackend = TargetBackend.JVM_IR,
                    excludedPattern = excludedCustomTestdataPattern
                )
            }

            testClass<AbstractDiagnosticsNativeTest> {
                model("diagnostics/nativeTests", excludedPattern = excludedCustomTestdataPattern)
            }

            testClass<AbstractDiagnosticsWithMultiplatformCompositeAnalysisTest> {
                model(
                    "diagnostics/testsWithMultiplatformCompositeAnalysis",
                    pattern = "^(.*)\\.kts?$", excludedPattern = excludedCustomTestdataPattern
                )
            }

            testClass<AbstractForeignAnnotationsSourceJavaTest> {
                model("diagnostics/foreignAnnotationsTests/tests", excludedPattern = excludedCustomTestdataPattern)
                model("diagnostics/foreignAnnotationsTests/java8Tests", excludedPattern = excludedCustomTestdataPattern)
                model("diagnostics/foreignAnnotationsTests/java11Tests", excludedPattern = excludedCustomTestdataPattern)
            }

            testClass<AbstractForeignAnnotationsCompiledJavaTest> {
                model("diagnostics/foreignAnnotationsTests/tests", excludedPattern = excludedCustomTestdataPattern)
                model("diagnostics/foreignAnnotationsTests/java8Tests", excludedPattern = excludedCustomTestdataPattern)
                model("diagnostics/foreignAnnotationsTests/java11Tests", excludedPattern = excludedCustomTestdataPattern)
            }

            testClass<AbstractForeignAnnotationsCompiledJavaWithPsiClassReadingTest> {
                model("diagnostics/foreignAnnotationsTests/tests", excludedPattern = excludedCustomTestdataPattern)
                model("diagnostics/foreignAnnotationsTests/java8Tests", excludedPattern = excludedCustomTestdataPattern)
                model("diagnostics/foreignAnnotationsTests/java11Tests", excludedPattern = excludedCustomTestdataPattern)
            }

            testClass<AbstractBlackBoxCodegenTest> {
                model("codegen/box")
            }

            testClass<AbstractIrBlackBoxCodegenTest> {
                model("codegen/box")
            }

            testClass<AbstractSteppingTest> {
                model("debug/stepping")
            }

            testClass<AbstractIrSteppingTest> {
                model("debug/stepping")
            }

            testClass<AbstractLocalVariableTest> {
                model("debug/localVariables")
            }

            testClass<AbstractIrLocalVariableTest> {
                model("debug/localVariables")
            }

            testClass<AbstractBlackBoxCodegenTest>("BlackBoxModernJdkCodegenTestGenerated") {
                model("codegen/boxModernJdk")
            }

            testClass<AbstractIrBlackBoxCodegenTest>("IrBlackBoxModernJdkCodegenTestGenerated") {
                model("codegen/boxModernJdk")
            }

            testClass<AbstractJvmIrAgainstOldBoxTest> {
                model("codegen/box/compileKotlinAgainstKotlin")
            }

            testClass<AbstractJvmOldAgainstIrBoxTest> {
                model("codegen/box/compileKotlinAgainstKotlin")
            }

            testClass<AbstractIrTextTest> {
                model("ir/irText")
            }

            testClass<AbstractBytecodeTextTest> {
                model("codegen/bytecodeText")
            }

            testClass<AbstractIrBytecodeTextTest> {
                model("codegen/bytecodeText")
            }

            testClass<AbstractBlackBoxInlineCodegenTest> {
                model("codegen/boxInline")
            }

            testClass<AbstractIrBlackBoxInlineCodegenTest> {
                model("codegen/boxInline")
            }

            testClass<AbstractCompileKotlinAgainstInlineKotlinTest> {
                model("codegen/boxInline")
            }

            testClass<AbstractIrCompileKotlinAgainstInlineKotlinTest> {
                model("codegen/boxInline")
            }

            testClass<AbstractIrSerializeCompileKotlinAgainstInlineKotlinTest> {
                model("codegen/boxInline")
            }

            testClass<AbstractJvmIrAgainstOldBoxInlineTest> {
                model("codegen/boxInline")
            }

            testClass<AbstractJvmOldAgainstIrBoxInlineTest> {
                model("codegen/boxInline")
            }

            testClass<AbstractBytecodeListingTest> {
                model("codegen/bytecodeListing")
            }

            testClass<AbstractIrBytecodeListingTest> {
                model("codegen/bytecodeListing")
            }

            testClass<AbstractAsmLikeInstructionListingTest> {
                model("codegen/asmLike")
            }

            testClass<AbstractIrAsmLikeInstructionListingTest> {
                model("codegen/asmLike")
            }

            testClass<AbstractJvmIrInterpreterAfterFir2IrTest> {
                model("ir/interpreter", excludeDirs = listOf("helpers"))
            }

            testClass<AbstractJvmIrInterpreterAfterPsi2IrTest> {
                model("ir/interpreter", excludeDirs = listOf("helpers"))
            }
        }

        // ---------------------------------------------- FIR tests ----------------------------------------------

        testGroup(testsRoot = "compiler/fir/analysis-tests/tests-gen", testDataRoot = "compiler/testData") {
            testClass<AbstractFirDiagnosticTest>(suiteTestClassName = "FirOldFrontendDiagnosticsTestGenerated") {
                model("diagnostics/tests", pattern = "^(.*)\\.kts?$", excludedPattern = excludedCustomTestdataPattern)
                model("diagnostics/testsWithStdLib", excludedPattern = excludedCustomTestdataPattern)
            }

            testClass<AbstractFirDiagnosticsWithLightTreeTest>(
                suiteTestClassName = "FirOldFrontendDiagnosticsWithLightTreeTestGenerated"
            ) {
                model("diagnostics/tests", excludedPattern = excludedCustomTestdataPattern)
                model("diagnostics/testsWithStdLib", excludedPattern = excludedCustomTestdataPattern)
            }

            testClass<AbstractFirForeignAnnotationsSourceJavaTest>(
                suiteTestClassName = "FirOldFrontendForeignAnnotationsSourceJavaTestGenerated"
            ) {
                model("diagnostics/foreignAnnotationsTests/tests", excludedPattern = excludedCustomTestdataPattern)
                model("diagnostics/foreignAnnotationsTests/java8Tests", excludedPattern = excludedCustomTestdataPattern)
                model("diagnostics/foreignAnnotationsTests/java11Tests", excludedPattern = excludedCustomTestdataPattern)
            }

            testClass<AbstractFirForeignAnnotationsCompiledJavaTest>(
                suiteTestClassName = "FirOldFrontendForeignAnnotationsCompiledJavaTestGenerated"
            ) {
                model("diagnostics/foreignAnnotationsTests/tests", excludedPattern = excludedCustomTestdataPattern)
                model("diagnostics/foreignAnnotationsTests/java8Tests", excludedPattern = excludedCustomTestdataPattern)
                model("diagnostics/foreignAnnotationsTests/java11Tests", excludedPattern = excludedCustomTestdataPattern)
            }

            testClass<AbstractFirForeignAnnotationsCompiledJavaWithPsiClassReadingTest>(
                suiteTestClassName = "FirOldFrontendForeignAnnotationsCompiledJavaWithPsiClassReadingTestGenerated"
            ) {
                model("diagnostics/foreignAnnotationsTests/tests", excludedPattern = excludedCustomTestdataPattern)
                model("diagnostics/foreignAnnotationsTests/java8Tests", excludedPattern = excludedCustomTestdataPattern)
                model("diagnostics/foreignAnnotationsTests/java11Tests", excludedPattern = excludedCustomTestdataPattern)
            }

            testClass<AbstractFirNativeDiagnosticsTest>(
                suiteTestClassName = "FirOldFrontendNativeDiagnosticsTestGenerated"
            ) {
                model("diagnostics/nativeTests", excludedPattern = excludedCustomTestdataPattern)
            }

            testClass<AbstractFirNativeDiagnosticsWithLightTreeTest>(
                suiteTestClassName = "FirOldFrontendNativeDiagnosticsWithLightTreeTestGenerated"
            ) {
                model("diagnostics/nativeTests", excludedPattern = excludedCustomTestdataPattern)
            }
        }

        testGroup(testsRoot = "compiler/fir/fir2ir/tests-gen", testDataRoot = "compiler/testData") {
            testClass<AbstractFirBlackBoxCodegenTest> {
                model("codegen/box")
            }

            testClass<AbstractFirPsiBlackBoxCodegenTest> {
                model("codegen/box")
            }

            testClass<AbstractFirBlackBoxCodegenTest>("FirBlackBoxModernJdkCodegenTestGenerated") {
                model("codegen/boxModernJdk")
            }

            testClass<AbstractFirPsiBlackBoxCodegenTest>("FirPsiBlackBoxModernJdkCodegenTestGenerated") {
                model("codegen/boxModernJdk")
            }

            testClass<AbstractFirBlackBoxInlineCodegenTest> {
                model("codegen/boxInline")
            }

            testClass<AbstractFirPsiBlackBoxInlineCodegenTest> {
                model("codegen/boxInline")
            }

            testClass<AbstractFirSteppingTest> {
                model("debug/stepping")
            }

            testClass<AbstractFirPsiSteppingTest> {
                model("debug/stepping")
            }

            testClass<AbstractFirLocalVariableTest> {
                model("debug/localVariables")
            }

            testClass<AbstractFirPsiLocalVariableTest> {
                model("debug/localVariables")
            }

            testClass<AbstractFirDiagnosticsTestWithJvmIrBackend> {
                model("diagnostics/firTestWithJvmBackend")
            }

            testClass<AbstractFirDiagnosticsTestWithJvmIrBackend>(suiteTestClassName = "FirOldDiagnosticsTestWithJvmIrBackendGenerated") {
                model("diagnostics/testsWithJvmBackend", excludedPattern = excludedCustomTestdataPattern)
            }

            testClass<AbstractFirSerializeCompileKotlinAgainstInlineKotlinTest> {
                model("codegen/boxInline")
            }

            testClass<AbstractFirPsiSerializeCompileKotlinAgainstInlineKotlinTest> {
                model("codegen/boxInline")
            }
        }

        testGroup(testsRoot = "compiler/fir/fir2ir/tests-gen", testDataRoot = "compiler/fir/fir2ir/testData") {
            testClass<AbstractFirBlackBoxCodegenTest>(
                suiteTestClassName = "FirSpecificBlackBoxCodegenTestGenerated"
            ) {
                model("codegen/box")
                model("codegen/boxWithStdLib")
            }

            testClass<AbstractFirPsiBlackBoxCodegenTest>(
                suiteTestClassName = "FirPsiSpecificBlackBoxCodegenTestGenerated"
            ) {
                model("codegen/box")
                model("codegen/boxWithStdLib")
            }

            testClass<AbstractFir2IrTextTest>(
                suiteTestClassName = "Fir2IrSpecificTextTestGenerated"
            ) {
                model("ir/irText")
            }

            testClass<AbstractFirPsi2IrTextTest>(
                suiteTestClassName = "LightTreeFir2IrSpecificTextTestGenerated"
            ) {
                model("ir/irText")
            }

            testClass<AbstractFirBytecodeListingTest>(
                suiteTestClassName = "Fir2IrSpecificBytecodeListingTestGenerated"
            ) {
                model("codegen/bytecodeListing")
            }

            testClass<AbstractFirPsiBytecodeListingTest>(
                suiteTestClassName = "Fir2IrSpecificBytecodeListingPsiTestGenerated"
            ) {
                model("codegen/bytecodeListing")
            }
        }

        testGroup("compiler/fir/analysis-tests/tests-gen", "compiler/fir/analysis-tests/testData") {
            testClass<AbstractFirDiagnosticTest> {
                model("resolve", pattern = TestGeneratorUtil.KT_WITHOUT_DOTS_IN_NAME)
                model("resolveWithStdlib", pattern = TestGeneratorUtil.KT_WITHOUT_DOTS_IN_NAME)
            }

            testClass<AbstractFirDiagnosticsWithLightTreeTest> {
                model("resolve", pattern = TestGeneratorUtil.KT_WITHOUT_DOTS_IN_NAME)
                model("resolveWithStdlib", pattern = TestGeneratorUtil.KT_WITHOUT_DOTS_IN_NAME)
            }
        }

        testGroup(testsRoot = "compiler/fir/fir2ir/tests-gen", testDataRoot = "compiler/testData") {
            testClass<AbstractFir2IrTextTest> {
                model("ir/irText")
            }

            testClass<AbstractFirPsi2IrTextTest> {
                model("ir/irText")
            }

            testClass<AbstractFirBytecodeTextTest> {
                model("codegen/bytecodeText")
            }

            testClass<AbstractFirPsiBytecodeTextTest> {
                model("codegen/bytecodeText")
            }
        }

        testGroup("compiler/visualizer/tests-gen", "compiler/fir/raw-fir/psi2fir/testData") {
            testClass<AbstractPsiVisualizerTest>("PsiVisualizerForRawFirDataGenerated") {
                model("rawBuilder")
            }

            testClass<AbstractFirVisualizerTest>("FirVisualizerForRawFirDataGenerated") {
                model("rawBuilder")
            }
        }

        testGroup("compiler/visualizer/tests-gen", "compiler/visualizer/testData") {
            testClass<AbstractPsiVisualizerTest>("PsiVisualizerForUncommonCasesGenerated") {
                model("uncommonCases/testFiles")
            }

            testClass<AbstractFirVisualizerTest>("FirVisualizerForUncommonCasesGenerated") {
                model("uncommonCases/testFiles")
            }
        }
    }
}
