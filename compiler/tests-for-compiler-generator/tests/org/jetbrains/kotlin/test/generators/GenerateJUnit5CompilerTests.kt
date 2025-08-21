/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.generators

import org.jetbrains.kotlin.generators.TestGroup.TestClass
import org.jetbrains.kotlin.generators.generateTestGroupSuiteWithJUnit5
import org.jetbrains.kotlin.generators.util.TestGeneratorUtil
import org.jetbrains.kotlin.generators.util.TestGeneratorUtil.canFreezeIDE
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
    val k1BoxTestDir = listOf("multiplatform/k1")
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

        // ---------------------------------------------- FIR tests ----------------------------------------------

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

        testGroup(testsRoot = "compiler/fir/fir2ir/tests-gen", testDataRoot = "compiler/testData") {
            testClass<AbstractFirLightTreeBlackBoxCodegenTest> {
                model("codegen/box", excludeDirs = k1BoxTestDir + excludedScriptDirs)
            }

            testClass<AbstractFirPsiBlackBoxCodegenTest> {
                model("codegen/box", excludeDirs = k1BoxTestDir)
            }
            testClass<AbstractJvmLightTreeBlackBoxCodegenWithSeparateKmpCompilationTest> {
                model("codegen/box/${k2BoxTestDir.first()}")
            }

            testClass<AbstractFirLightTreeBlackBoxCodegenTest>("FirLightTreeBlackBoxModernJdkCodegenTestGenerated") {
                model("codegen/boxModernJdk")
            }

            testClass<AbstractFirPsiBlackBoxCodegenTest>("FirPsiBlackBoxModernJdkCodegenTestGenerated") {
                model("codegen/boxModernJdk")
            }

            testClass<AbstractFirPsiBlackBoxInlineCodegenTest> {
                model("codegen/boxInline")
            }

            testClass<AbstractFirLightTreeBlackBoxInlineCodegenTest> {
                model("codegen/boxInline")
                model("klib/syntheticAccessors")
            }

            testClass<AbstractComposeLikeIrBlackBoxCodegenTest> {
                model("codegen/composeLike")
            }

            testClass<AbstractComposeLikeFirLightTreeBlackBoxCodegenTest> {
                model("codegen/composeLike")
            }

            testClass<AbstractComposeLikeIrBytecodeTextTest> {
                model("codegen/composeLikeBytecodeText")
            }

            testClass<AbstractComposeLikeFirLightTreeBytecodeTextTest> {
                model("codegen/composeLikeBytecodeText")
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

            testClass<AbstractFirPsiDiagnosticsTestWithConverter> {
                model(
                    "diagnostics/testsWithConverter",
                    pattern = "^(.+)\\.kts?$",
                    excludedPattern = excludedCustomTestdataPattern
                )
            }

            testClass<AbstractFirPsiDiagnosticsTestWithJvmIrBackend> {
                model("diagnostics/testsWithJvmBackend", excludedPattern = excludedCustomTestdataPattern)
            }

            testClass<AbstractFirLightTreeDiagnosticsTestWithJvmIrBackend> {
                model("diagnostics/testsWithJvmBackend", excludedPattern = excludedCustomTestdataPattern)
            }

            testClass<AbstractFirLightTreeSerializeCompileKotlinAgainstInlineKotlinTest> {
                model("codegen/box")
                model("codegen/boxInline")
            }

            testClass<AbstractFirPsiSerializeCompileKotlinAgainstInlineKotlinTest> {
                model("codegen/box")
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

        // ---------------------------------------------- Tiered tests ----------------------------------------------

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
    }
}
