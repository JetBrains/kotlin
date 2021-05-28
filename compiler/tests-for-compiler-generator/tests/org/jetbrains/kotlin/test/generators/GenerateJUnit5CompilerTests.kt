/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.generators

import org.jetbrains.kotlin.generators.model.annotation
import org.jetbrains.kotlin.generators.util.TestGeneratorUtil
import org.jetbrains.kotlin.test.TargetBackend
import org.jetbrains.kotlin.test.runners.*
import org.jetbrains.kotlin.test.runners.codegen.*
import org.jetbrains.kotlin.test.runners.ir.AbstractFir2IrTextTest
import org.jetbrains.kotlin.test.runners.ir.AbstractIrTextTest
import org.jetbrains.kotlin.test.runners.ir.interpreter.AbstractIrInterpreterAfterFir2IrTest
import org.jetbrains.kotlin.test.runners.ir.interpreter.AbstractIrInterpreterAfterPsi2IrTest
import org.jetbrains.kotlin.visualizer.fir.AbstractFirVisualizerTest
import org.jetbrains.kotlin.visualizer.psi.AbstractPsiVisualizerTest
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode

fun generateJUnit5CompilerTests(args: Array<String>) {
    val excludedFirTestdataPattern = "^(.+)\\.fir\\.kts?\$"

    generateTestGroupSuiteWithJUnit5(args) {
        testGroup(testsRoot = "compiler/tests-common-new/tests-gen", testDataRoot = "compiler/testData") {
            testClass<AbstractDiagnosticTest> {
                model("diagnostics/tests", pattern = "^(.*)\\.kts?$", excludedPattern = excludedFirTestdataPattern)
                model("diagnostics/testsWithStdLib", excludedPattern = excludedFirTestdataPattern)
            }

            testClass<AbstractDiagnosticUsingJavacTest> {
                model("diagnostics/tests/javac", pattern = "^(.*)\\.kts?$", excludedPattern = excludedFirTestdataPattern)
            }

            testClass<AbstractDiagnosticsTestWithJsStdLib> {
                model("diagnostics/testsWithJsStdLib")
            }

            testClass<AbstractDiagnosticsTestWithOldJvmBackend> {
                model("diagnostics/testsWithJvmBackend", targetBackend = TargetBackend.JVM_OLD)
            }

            testClass<AbstractDiagnosticsTestWithJvmIrBackend> {
                model("diagnostics/testsWithJvmBackend", targetBackend = TargetBackend.JVM_IR)
            }

            testClass<AbstractDiagnosticsNativeTest> {
                model("diagnostics/nativeTests")
            }

            testClass<AbstractForeignAnnotationsSourceJavaTest> {
                model("diagnostics/foreignAnnotationsTests/tests")
                model("diagnostics/foreignAnnotationsTests/java8Tests")
                model("diagnostics/foreignAnnotationsTests/java9Tests")
            }

            testClass<AbstractForeignAnnotationsCompiledJavaTest> {
                model("diagnostics/foreignAnnotationsTests/tests")
                model("diagnostics/foreignAnnotationsTests/java8Tests")
                model("diagnostics/foreignAnnotationsTests/java9Tests")
            }

            testClass<AbstractForeignAnnotationsCompiledJavaWithPsiClassReadingTest> {
                model("diagnostics/foreignAnnotationsTests/tests")
                model("diagnostics/foreignAnnotationsTests/java8Tests",)
                model("diagnostics/foreignAnnotationsTests/java9Tests")
            }

            testClass<AbstractBlackBoxCodegenTest> {
                model("codegen/box")
            }

            testClass<AbstractIrBlackBoxCodegenTest> {
                model("codegen/box")
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
        }

        // ---------------------------------------------- FIR tests ----------------------------------------------

        testGroup(testsRoot = "compiler/fir/analysis-tests/tests-gen", testDataRoot = "compiler/testData") {
            testClass<AbstractFirDiagnosticTest>(suiteTestClassName = "FirOldFrontendDiagnosticsTestGenerated") {
                model("diagnostics/tests", excludedPattern = excludedFirTestdataPattern)
                model("diagnostics/testsWithStdLib", excludedPattern = excludedFirTestdataPattern)
            }

            testClass<AbstractFirDiagnosticsWithLightTreeTest>(
                suiteTestClassName = "FirOldFrontendDiagnosticsWithLightTreeTestGenerated"
            ) {
                model("diagnostics/tests", excludedPattern = excludedFirTestdataPattern)
                model("diagnostics/testsWithStdLib", excludedPattern = excludedFirTestdataPattern)
            }
        }

        testGroup(testsRoot = "compiler/fir/fir2ir/tests-gen", testDataRoot = "compiler/testData") {
            testClass<AbstractFirBlackBoxCodegenTest> {
                model("codegen/box")
            }

            testClass<AbstractFirBlackBoxInlineCodegenTest> {
                model("codegen/boxInline")
            }

            testClass<AbstractIrInterpreterAfterFir2IrTest> {
                model("ir/interpreter", excludeDirs = listOf("helpers"))
            }

            testClass<AbstractIrInterpreterAfterPsi2IrTest> {
                model("ir/interpreter", excludeDirs = listOf("helpers"))
            }
        }

        testGroup(testsRoot = "compiler/fir/fir2ir/tests-gen", testDataRoot = "compiler/fir/fir2ir/testData") {
            testClass<AbstractFirBlackBoxCodegenTest>(
                suiteTestClassName = "FirSpecificBlackBoxCodegenTestGenerated"
            ) {
                model("codegen/box")
            }

            testClass<AbstractFir2IrTextTest>(
                suiteTestClassName = "Fir2IrSpecificTextTestGenerated"
            ) {
                model("ir/irText")
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

            testClass<AbstractFirBytecodeTextTest> {
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
