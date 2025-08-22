/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test

import org.jetbrains.kotlin.generators.generateTestGroupSuiteWithJUnit5
import org.jetbrains.kotlin.generators.util.TestGeneratorUtil
import org.jetbrains.kotlin.test.runners.AbstractClassicDiagnosticsTestWithConverter
import org.jetbrains.kotlin.test.runners.AbstractClassicJvmIntegrationDiagnosticTest
import org.jetbrains.kotlin.test.runners.AbstractDiagnosticTest
import org.jetbrains.kotlin.test.runners.AbstractDiagnosticUsingJavacTest
import org.jetbrains.kotlin.test.runners.AbstractDiagnosticsTestWithJvmIrBackend
import org.jetbrains.kotlin.test.runners.AbstractDiagnosticsWithMultiplatformCompositeAnalysisTest
import org.jetbrains.kotlin.test.runners.AbstractForeignAnnotationsCompiledJavaTest
import org.jetbrains.kotlin.test.runners.AbstractForeignAnnotationsCompiledJavaWithPsiClassReadingTest
import org.jetbrains.kotlin.test.runners.AbstractForeignAnnotationsSourceJavaTest
import org.jetbrains.kotlin.test.runners.AbstractJvmAbiConsistencyTest
import org.jetbrains.kotlin.test.runners.codegen.AbstractIrAsmLikeInstructionListingTest
import org.jetbrains.kotlin.test.runners.codegen.AbstractIrBlackBoxCodegenTest
import org.jetbrains.kotlin.test.runners.codegen.AbstractIrBlackBoxInlineCodegenTest
import org.jetbrains.kotlin.test.runners.codegen.AbstractIrBytecodeListingTest
import org.jetbrains.kotlin.test.runners.codegen.AbstractIrBytecodeTextTest
import org.jetbrains.kotlin.test.runners.codegen.AbstractIrCompileKotlinAgainstInlineKotlinTest
import org.jetbrains.kotlin.test.runners.codegen.AbstractIrLocalVariableTest
import org.jetbrains.kotlin.test.runners.codegen.AbstractIrSerializeCompileKotlinAgainstInlineKotlinTest
import org.jetbrains.kotlin.test.runners.codegen.AbstractIrSteppingTest
import org.jetbrains.kotlin.test.runners.codegen.inlineScopes.AbstractFirBlackBoxCodegenTestWithInlineScopes
import org.jetbrains.kotlin.test.runners.codegen.inlineScopes.AbstractFirBlackBoxInlineCodegenTestWithInlineScopes
import org.jetbrains.kotlin.test.runners.codegen.inlineScopes.AbstractFirBytecodeTextTestWithInlineScopes
import org.jetbrains.kotlin.test.runners.codegen.inlineScopes.AbstractFirLocalVariableTestWithInlineScopes
import org.jetbrains.kotlin.test.runners.codegen.inlineScopes.AbstractFirSerializeCompileKotlinAgainstInlineKotlinTestWithInlineScopes
import org.jetbrains.kotlin.test.runners.codegen.inlineScopes.AbstractFirSteppingTestWithInlineScopes
import org.jetbrains.kotlin.test.runners.ir.AbstractClassicJvmIrSourceRangesTest
import org.jetbrains.kotlin.test.runners.ir.AbstractClassicJvmIrTextTest
import org.jetbrains.kotlin.test.runners.ir.interpreter.AbstractJvmIrInterpreterAfterFirPsi2IrTest
import org.jetbrains.kotlin.test.runners.ir.interpreter.AbstractJvmIrInterpreterAfterPsi2IrTest
import org.jetbrains.kotlin.test.utils.CUSTOM_TEST_DATA_EXTENSION_PATTERN

fun main(args: Array<String>) {
    val mainClassName = TestGeneratorUtil.getMainClassName()
    val excludedCustomTestdataPattern = CUSTOM_TEST_DATA_EXTENSION_PATTERN
    val k1BoxTestDir = listOf("multiplatform/k1")
    val k2BoxTestDir = listOf("multiplatform/k2")
    val excludedScriptDirs = listOf("script")

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

            testClass<AbstractClassicDiagnosticsTestWithConverter> {
                model(
                    "diagnostics/testsWithConverter",
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

            testClass<AbstractIrBlackBoxCodegenTest> {
                model("codegen/box", excludeDirs = k2BoxTestDir)
            }


            // We split JVM ABI tests into two parts, to avoid creation of a huge file, unable to analyze by IntelliJ with default settings
            testClass<AbstractJvmAbiConsistencyTest>("JvmAbiConsistencyTestBoxGenerated") {
                model("codegen/box", excludeDirs = listOf("multiplatform"))
            }

            testClass<AbstractJvmAbiConsistencyTest>("JvmAbiConsistencyTestRestGenerated") {
                model("codegen/boxInline")
                model("codegen/boxModernJdk")
                model("codegen/bytecodeText")
                model("codegen/bytecodeListing")
                model("codegen/composeLike")
                model("codegen/composeLikeBytecodeText")
                model("codegen/script", pattern = "^(.*)\\.kts?$", excludedPattern = excludedCustomTestdataPattern)
            }

            testClass<AbstractIrSteppingTest> {
                model("debug/stepping")
            }

            testClass<AbstractIrLocalVariableTest> {
                model("debug/localVariables")
            }

            testClass<AbstractIrBlackBoxCodegenTest>("IrBlackBoxModernJdkCodegenTestGenerated") {
                model("codegen/boxModernJdk")
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

            testClass<AbstractIrBytecodeTextTest> {
                model("codegen/bytecodeText")
            }

            testClass<AbstractIrBlackBoxInlineCodegenTest> {
                model("codegen/boxInline")
                model("klib/syntheticAccessors")
            }

            testClass<AbstractIrCompileKotlinAgainstInlineKotlinTest> {
                model("codegen/boxInline")
            }

            testClass<AbstractIrSerializeCompileKotlinAgainstInlineKotlinTest> {
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

            testClass<AbstractClassicJvmIntegrationDiagnosticTest> {
                model("diagnostics/jvmIntegration", pattern = TestGeneratorUtil.KT_WITHOUT_DOTS_IN_NAME)
            }

            // ------------- Inline scopes tests duplication -------------

            testClass<AbstractFirBlackBoxCodegenTestWithInlineScopes> {
                model("codegen/box", excludeDirs = k1BoxTestDir + excludedScriptDirs)
            }

            testClass<AbstractFirBytecodeTextTestWithInlineScopes> {
                model("codegen/bytecodeText")
            }

            testClass<AbstractFirSteppingTestWithInlineScopes> {
                model("debug/stepping")
            }

            testClass<AbstractFirLocalVariableTestWithInlineScopes> {
                model("debug/localVariables", excludeDirs = inlineScopesNewFormatToOld)
            }

            testClass<AbstractFirBlackBoxInlineCodegenTestWithInlineScopes> {
                model("codegen/boxInline", excludeDirs = k2BoxTestDir)
            }

            testClass<AbstractFirSerializeCompileKotlinAgainstInlineKotlinTestWithInlineScopes> {
                model("codegen/box")
                model("codegen/boxInline")
            }

            testClass<AbstractFirBlackBoxCodegenTestWithInlineScopes>("FirBlackBoxModernJdkCodegenTestGeneratedWithInlineScopes") {
                model("codegen/boxModernJdk")
            }
        }
    }
}
