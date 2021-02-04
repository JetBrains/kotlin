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
import org.jetbrains.kotlin.visualizer.fir.AbstractFirVisualizer
import org.jetbrains.kotlin.visualizer.psi.AbstractPsiVisualizer
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

            testClass<AbstractForeignAnnotationsTest> {
                model("foreignAnnotations/tests")
                model("foreignAnnotations/java8Tests", excludeDirs = listOf("jspecify", "typeEnhancementOnCompiledJava"))
            }

            testClass<AbstractForeignAnnotationsNoAnnotationInClasspathTest> {
                model("foreignAnnotations/tests")
                model("foreignAnnotations/java8Tests", excludeDirs = listOf("jspecify", "typeEnhancementOnCompiledJava"))
            }

            testClass<AbstractForeignAnnotationsNoAnnotationInClasspathWithPsiClassReadingTest> {
                model("foreignAnnotations/tests")
                model("foreignAnnotations/java8Tests", excludeDirs = listOf("jspecify", "typeEnhancementOnCompiledJava"))
            }

            testClass<AbstractBlackBoxCodegenTest> {
                model("codegen/box")
            }

            testClass<AbstractIrBlackBoxCodegenTest> {
                model("codegen/box")
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

            testClass<AbstractJvmIrAgainstOldBoxInlineTest> {
                model("codegen/boxInline")
            }

            testClass<AbstractJvmOldAgainstIrBoxInlineTest> {
                model("codegen/boxInline")
            }
        }

        // ---------------------------------------------- FIR tests ----------------------------------------------

        testGroup(testsRoot = "compiler/fir/analysis-tests/tests-gen", testDataRoot = "compiler/testData") {
            testClass<AbstractFirDiagnosticTest>(suiteTestClassName = "FirOldFrontendDiagnosticsTestGenerated") {
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
        }

        testGroup("compiler/fir/analysis-tests/tests-gen", "compiler/fir/analysis-tests/testData") {
            testClass<AbstractFirDiagnosticTest> {
                model("resolve", pattern = TestGeneratorUtil.KT_WITHOUT_DOTS_IN_NAME)
                model("resolveWithStdlib", pattern = TestGeneratorUtil.KT_WITHOUT_DOTS_IN_NAME)
            }

            testClass<AbstractFirDiagnosticsWithLightTreeTest>(
                annotations = listOf(annotation(Execution::class.java, ExecutionMode.SAME_THREAD))
            ) {
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
            testClass<AbstractPsiVisualizer>("PsiVisualizerForRawFirDataGenerated") {
                model("rawBuilder")
            }

            testClass<AbstractFirVisualizer>("FirVisualizerForRawFirDataGenerated") {
                model("rawBuilder")
            }
        }

        testGroup("compiler/visualizer/tests-gen", "compiler/visualizer/testData") {
            testClass<AbstractPsiVisualizer>("PsiVisualizerForUncommonCasesGenerated") {
                model("uncommonCases/testFiles")
            }

            testClass<AbstractFirVisualizer>("FirVisualizerForUncommonCasesGenerated") {
                model("uncommonCases/testFiles")
            }
        }
    }
}
