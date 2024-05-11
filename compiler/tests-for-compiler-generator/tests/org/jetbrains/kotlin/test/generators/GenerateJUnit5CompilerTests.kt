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
import org.jetbrains.kotlin.test.runners.codegen.inlineScopes.*
import org.jetbrains.kotlin.test.runners.ir.*
import org.jetbrains.kotlin.test.runners.ir.interpreter.AbstractJvmIrInterpreterAfterFirPsi2IrTest
import org.jetbrains.kotlin.test.runners.ir.interpreter.AbstractJvmIrInterpreterAfterPsi2IrTest
import org.jetbrains.kotlin.test.utils.CUSTOM_TEST_DATA_EXTENSION_PATTERN

fun generateJUnit5CompilerTests(args: Array<String>, mainClassName: String?) {
    val excludedCustomTestdataPattern = CUSTOM_TEST_DATA_EXTENSION_PATTERN
    val k2BoxTestDir = listOf("multiplatform/k2")
    val excludedScriptDirs = listOf("script")
    // We exclude the 'inlineScopes' directory from the IR inliner tests. The reason is that
    // the IR inliner produces slightly different bytecode than the bytecode inliner (see KT-65477).
    val inlineScopesTestDir = listOf("inlineScopes")
    // We exclude the 'inlineScopes/newFormatToOld' directory from tests that have inline scopes enabled
    // by default, since we only want to test the scenario where code with inline scopes is inlined by the
    // old inliner with $iv suffixes.
    val inlineScopesNewFormatToOld = listOf("inlineScopes/newFormatToOld")

    generateTestGroupSuiteWithJUnit5(args, mainClassName) {
        testGroup(testsRoot = "compiler/tests-common-new/tests-gen", testDataRoot = "compiler/testData") {
            testClass<AbstractDiagnosticTest> {
                model("diagnostics/tests", pattern = "^(.*)\\.kts?$", excludedPattern = excludedCustomTestdataPattern)
                model("diagnostics/testsWithStdLib", excludedPattern = excludedCustomTestdataPattern)
            }

            testClass<AbstractDiagnosticUsingJavacTest> {
                model("diagnostics/tests/javac", pattern = "^(.*)\\.kts?$", excludedPattern = excludedCustomTestdataPattern)
            }

            testClass<AbstractDiagnosticsTestWithJvmIrBackend> {
                model(
                    "diagnostics/testsWithJvmBackend",
                    pattern = "^(.+)\\.kts?$",
                    targetBackend = TargetBackend.JVM_IR,
                    excludedPattern = excludedCustomTestdataPattern
                )
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
                model(
                    "diagnostics/foreignAnnotationsTests/tests",
                    excludedPattern = excludedCustomTestdataPattern,
                    excludeDirs = listOf("externalAnnotations"),
                )
                model("diagnostics/foreignAnnotationsTests/java8Tests", excludedPattern = excludedCustomTestdataPattern)
                model("diagnostics/foreignAnnotationsTests/java11Tests", excludedPattern = excludedCustomTestdataPattern)
            }

            testClass<AbstractForeignAnnotationsCompiledJavaWithPsiClassReadingTest> {
                model("diagnostics/foreignAnnotationsTests/tests", excludedPattern = excludedCustomTestdataPattern)
                model("diagnostics/foreignAnnotationsTests/java8Tests", excludedPattern = excludedCustomTestdataPattern)
                model("diagnostics/foreignAnnotationsTests/java11Tests", excludedPattern = excludedCustomTestdataPattern)
            }

            testClass<AbstractBlackBoxCodegenTest> {
                model("codegen/box", excludeDirs = k2BoxTestDir)
            }

            testClass<AbstractIrBlackBoxCodegenTest> {
                model("codegen/box", excludeDirs = k2BoxTestDir)
            }


            // We split JVM ABI tests into two parts, to avoid creation of a huge file, unable to analyze by IntelliJ with default settings
            testClass<AbstractJvmAbiConsistencyTest>("JvmAbiConsistencyTestBoxGenerated") {
                model("codegen/box", excludeDirs = k2BoxTestDir)
            }

            testClass<AbstractJvmAbiConsistencyTest>("JvmAbiConsistencyTestRestGenerated") {
                model("codegen/boxInline")
                model("codegen/boxModernJdk")
                model("codegen/bytecodeText")
                model("codegen/bytecodeListing")
                model("codegen/composeLike")
                model("codegen/composeLikeBytecodeText")
                model("codegen/defaultArguments")
                model("codegen/script", pattern = "^(.*)\\.kts?$", excludedPattern = excludedCustomTestdataPattern)
            }

            testClass<AbstractIrBlackBoxCodegenWithIrInlinerTest> {
                model("codegen/box", excludeDirs = k2BoxTestDir)
            }

            testClass<AbstractIrSteppingWithBytecodeInlinerTest> {
                model("debug/stepping")
            }

            testClass<AbstractIrSteppingWithIrInlinerTest> {
                model("debug/stepping")
            }

            testClass<AbstractIrLocalVariableBytecodeInlinerTest> {
                model("debug/localVariables")
            }

            testClass<AbstractIrLocalVariableIrInlinerTest> {
                model("debug/localVariables", excludeDirs = inlineScopesTestDir)
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

            testClass<AbstractClassicJvmIrTextTest> {
                model(
                    "ir/irText",
                    excludeDirs = listOf("declarations/multiplatform/k2")
                )
            }

            testClass<AbstractClassicJvmIrSourceRangesTest> {
                model("ir/sourceRanges")
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

            testClass<AbstractIrBlackBoxInlineCodegenWithBytecodeInlinerTest> {
                model("codegen/boxInline")
            }

            testClass<AbstractIrBlackBoxInlineCodegenWithIrInlinerTest> {
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

            testClass<AbstractIrBytecodeListingTest> {
                model("codegen/bytecodeListing")
            }

            testClass<AbstractIrAsmLikeInstructionListingTest> {
                model("codegen/asmLike")
            }

            testClass<AbstractJvmIrInterpreterAfterFirPsi2IrTest> {
                model("ir/interpreter", excludeDirs = listOf("helpers"))
            }

            testClass<AbstractJvmIrInterpreterAfterPsi2IrTest> {
                model("ir/interpreter", excludeDirs = listOf("helpers"))
            }

            // ------------- Inline scopes tests duplication -------------

            testClass<AbstractFirBlackBoxCodegenTestWithInlineScopes> {
                model("codegen/box", excludeDirs = k2BoxTestDir + excludedScriptDirs)
            }

            testClass<AbstractFirBytecodeTextTestWithInlineScopes> {
                model("codegen/bytecodeText")
            }

            testClass<AbstractFirSteppingWithBytecodeInlinerTestWithInlineScopes> {
                model("debug/stepping")
            }

            testClass<AbstractFirSteppingWithIrInlinerTestWithInlineScopes> {
                model("debug/stepping")
            }

            testClass<AbstractFirLocalVariableBytecodeInlinerTestWithInlineScopes> {
                model("debug/localVariables", excludeDirs = inlineScopesNewFormatToOld)
            }

            testClass<AbstractFirLocalVariableIrInlinerTestWithInlineScopes> {
                model("debug/localVariables", excludeDirs = inlineScopesTestDir)
            }

            testClass<AbstractFirBlackBoxInlineCodegenWithBytecodeInlinerTestWithInlineScopes> {
                model("codegen/boxInline", excludeDirs = k2BoxTestDir)
            }

            testClass<AbstractFirBlackBoxInlineCodegenWithIrInlinerTestWithInlineScopes> {
                model("codegen/boxInline")
            }

            testClass<AbstractFirSerializeCompileKotlinAgainstInlineKotlinTestWithInlineScopes> {
                model("codegen/boxInline")
            }

            testClass<AbstractFirBlackBoxCodegenTestWithInlineScopes>("FirBlackBoxModernJdkCodegenTestGeneratedWithInlineScopes") {
                model("codegen/boxModernJdk")
            }
        }

        // ---------------------------------------------- FIR tests ----------------------------------------------

        testGroup(testsRoot = "compiler/fir/analysis-tests/tests-gen", testDataRoot = "compiler/testData") {
            testClass<AbstractFirPsiDiagnosticTest>(suiteTestClassName = "FirPsiOldFrontendDiagnosticsTestGenerated") {
                model(
                    "diagnostics/tests", pattern = "^(.*)\\.kts?$",
                    excludeDirsRecursively = listOf("multiplatform"),
                    excludedPattern = excludedCustomTestdataPattern
                )
                model("diagnostics/testsWithStdLib", excludedPattern = excludedCustomTestdataPattern)
            }

            testClass<AbstractFirPsiWithActualizerDiagnosticsTest>(suiteTestClassName = "FirOldFrontendMPPDiagnosticsWithPsiTestGenerated") {
                model("diagnostics/tests/multiplatform", pattern = "^(.*)\\.kts?$", excludedPattern = excludedCustomTestdataPattern)
            }

            testClass<AbstractFirLightTreeDiagnosticsTest>(
                suiteTestClassName = "FirLightTreeOldFrontendDiagnosticsTestGenerated"
            ) {
                model(
                    "diagnostics/tests",
                    excludeDirsRecursively = listOf("multiplatform"),
                    excludedPattern = excludedCustomTestdataPattern
                )
                model("diagnostics/testsWithStdLib", excludedPattern = excludedCustomTestdataPattern)
            }

            testClass<AbstractFirLightTreeWithActualizerDiagnosticsTest>(suiteTestClassName = "FirOldFrontendMPPDiagnosticsWithLightTreeTestGenerated") {
                model("diagnostics/tests/multiplatform", pattern = "^(.*)\\.kts?$", excludedPattern = excludedCustomTestdataPattern)
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

        testGroup("compiler/fir/analysis-tests/tests-gen", "compiler/testData") {
            testClass<AbstractFirLoadK1CompiledJvmKotlinTest> {
                model("loadJava/compiledKotlin", extension = "kt")
                model("loadJava/compiledKotlinWithStdlib", extension = "kt")
            }

            testClass<AbstractFirLoadK2CompiledJvmKotlinTest> {
                model("loadJava/compiledKotlin", extension = "kt")
                model("loadJava/compiledKotlinWithStdlib", extension = "kt")
            }
        }

        testGroup(testsRoot = "compiler/fir/fir2ir/tests-gen", testDataRoot = "compiler/testData") {
            testClass<AbstractFirLightTreeBlackBoxCodegenTest> {
                model("codegen/box", excludeDirs = excludedScriptDirs)
            }

            testClass<AbstractFirPsiBlackBoxCodegenTest> {
                model("codegen/box")
            }

            testClass<AbstractFirLightTreeBlackBoxCodegenTest>("FirLightTreeBlackBoxModernJdkCodegenTestGenerated") {
                model("codegen/boxModernJdk")
            }

            testClass<AbstractFirPsiBlackBoxCodegenTest>("FirPsiBlackBoxModernJdkCodegenTestGenerated") {
                model("codegen/boxModernJdk")
            }

            testClass<AbstractFirPsiBlackBoxInlineCodegenWithBytecodeInlinerTest> {
                model("codegen/boxInline")
            }

            testClass<AbstractFirPsiBlackBoxInlineCodegenWithIrInlinerTest> {
                model("codegen/boxInline")
            }

            testClass<AbstractFirLightTreeBlackBoxInlineCodegenWithBytecodeInlinerTest> {
                model("codegen/boxInline")
            }

            testClass<AbstractFirLightTreeBlackBoxInlineCodegenWithIrInlinerTest> {
                model("codegen/boxInline")
            }

            testClass<AbstractFirLightTreeSteppingTest> {
                model("debug/stepping")
            }

            testClass<AbstractFirPsiSteppingTest> {
                model("debug/stepping")
            }

            testClass<AbstractFirLightTreeLocalVariableTest> {
                model("debug/localVariables")
            }

            testClass<AbstractFirPsiLocalVariableTest> {
                model("debug/localVariables")
            }

            testClass<AbstractFirPsiWithInterpreterDiagnosticsTest> {
                model("diagnostics/irInterpreter")
            }

            testClass<AbstractFirLightTreeWithInterpreterDiagnosticsTest> {
                model("diagnostics/irInterpreter")
            }

            testClass<AbstractFirPsiDiagnosticsTestWithJvmIrBackend> {
                model("diagnostics/testsWithJvmBackend", excludedPattern = excludedCustomTestdataPattern)
            }

            testClass<AbstractFirLightTreeDiagnosticsTestWithJvmIrBackend> {
                model("diagnostics/testsWithJvmBackend", excludedPattern = excludedCustomTestdataPattern)
            }

            testClass<AbstractFirLightTreeSerializeCompileKotlinAgainstInlineKotlinTest> {
                model("codegen/boxInline")
            }

            testClass<AbstractFirPsiSerializeCompileKotlinAgainstInlineKotlinTest> {
                model("codegen/boxInline")
            }

            testClass<AbstractFirPsiBytecodeListingTest> {
                model("codegen/bytecodeListing")
            }

            testClass<AbstractFirLightTreeBytecodeListingTest> {
                model("codegen/bytecodeListing")
            }

            testClass<AbstractFirPsiAsmLikeInstructionListingTest> {
                model("codegen/asmLike")
            }

            testClass<AbstractFirLightTreeAsmLikeInstructionListingTest> {
                model("codegen/asmLike")
            }

            testClass<AbstractFirScriptCodegenTest> {
                model("codegen/script", pattern = "^(.*)\\.kts?$", excludedPattern = excludedCustomTestdataPattern)
            }
        }

        testGroup("compiler/fir/analysis-tests/tests-gen", "compiler/fir/analysis-tests/testData") {
            testClass<AbstractFirPsiDiagnosticTest> {
                model("resolve", pattern = TestGeneratorUtil.KT_OR_KTS_WITHOUT_DOTS_IN_NAME)
                model("resolveWithStdlib", pattern = TestGeneratorUtil.KT_OR_KTS_WITHOUT_DOTS_IN_NAME)
            }

            testClass<AbstractFirLightTreeDiagnosticsTest> {
                model("resolve", pattern = TestGeneratorUtil.KT_WITHOUT_DOTS_IN_NAME)
                model("resolveWithStdlib", pattern = TestGeneratorUtil.KT_WITHOUT_DOTS_IN_NAME)
            }
        }

        testGroup(testsRoot = "compiler/fir/fir2ir/tests-gen", testDataRoot = "compiler/testData") {
            testClass<AbstractFirLightTreeJvmIrTextTest> {
                model(
                    "ir/irText",
                    excludeDirs = listOf("declarations/multiplatform/k1")
                )
            }

            testClass<AbstractFirPsiJvmIrTextTest> {
                model(
                    "ir/irText",
                    excludeDirs = listOf("declarations/multiplatform/k1")
                )
            }

            testClass<AbstractFirLightTreeJvmIrSourceRangesTest> {
                model("ir/sourceRanges")
            }

            testClass<AbstractFirPsiJvmIrSourceRangesTest> {
                model("ir/sourceRanges")
            }

            testClass<AbstractFirLightTreeBytecodeTextTest> {
                model("codegen/bytecodeText")
            }

            testClass<AbstractFirPsiBytecodeTextTest> {
                model("codegen/bytecodeText")
            }
        }
    }
}
