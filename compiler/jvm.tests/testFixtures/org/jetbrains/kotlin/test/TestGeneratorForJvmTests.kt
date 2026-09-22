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
import org.jetbrains.kotlin.test.runners.codegen.*
import org.jetbrains.kotlin.test.runners.codegen.inlineScopes.*
import org.jetbrains.kotlin.test.runners.ir.AbstractFirLightTreeJvmIrSourceRangesTest
import org.jetbrains.kotlin.test.runners.ir.AbstractFirLightTreeJvmIrTextTest
import org.jetbrains.kotlin.test.runners.ir.AbstractFirPsiJvmIrSourceRangesTest
import org.jetbrains.kotlin.test.runners.ir.AbstractFirPsiJvmIrTextTest
import org.jetbrains.kotlin.test.utils.CUSTOM_TEST_DATA_EXTENSION_PATTERN

fun main(args: Array<String>) {
    val mainClassName = TestGeneratorUtil.getMainClassName()
    val testRoot = args[0]
    val excludedCustomTestdataPattern = CUSTOM_TEST_DATA_EXTENSION_PATTERN

    generateTestGroupSuiteWithJUnit5(args, mainClassName) {
        testGroup(testRoot, testDataRoot = "compiler/testData") {
            testClass<AbstractFirLightTreeWithActualizerDiagnosticsWithLatestLanguageVersionTest>(suiteTestClassName = "FirOldFrontendMPPDiagnosticsWithLightTreeWithLatestLanguageVersionTestGenerated") {
                model("diagnostics/tests/multiplatform", pattern = "^(.*)\\.kts?$", excludedPattern = excludedCustomTestdataPattern)
            }

            run {
                val init: TestClass.() -> Unit = {
                    model(
                        "diagnostics/tests", pattern = TestGeneratorUtil.KT,
                        excludeDirsRecursively = listOf("multiplatform", "jvm"),
                        excludedPattern = excludedCustomTestdataPattern,
                    )
                    model(
                        "diagnostics/testsWithStdLib",
                        excludedPattern = excludedCustomTestdataPattern,
                    )
                }

                testClass<AbstractFirLightTreeDiagnosticsWithLatestLanguageVersionTest>(
                    init = init
                )

                testClass<AbstractFirLightTreeDiagnosticsWithLanguageFeatureDisabledTest>(
                    init = init
                )

                testClass<AbstractFirLightTreeDiagnosticsWithoutAliasExpansionTest>(
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
            testClass<AbstractFirLoadK2CompiledJvmKotlinTest> {
                model("loadJava/compiledKotlin", extension = "kt")
                model("loadJava/compiledKotlinWithStdlib", extension = "kt")
            }

            testClass<AbstractFirLoadCompiledJvmWithAnnotationsInMetadataKotlinTest> {
                model("loadJava/compiledKotlin", extension = "kt")
                model("loadJava/compiledKotlinWithStdlib", extension = "kt")
            }
        }

        testGroup(testRoot, "compiler/") {
            fun TestClass.phasedModel(allowKts: Boolean, excludeDirsRecursively: List<String> = emptyList()) {
                val relativeRootPaths = listOf(
                    "testData/diagnostics/tests",
                    "testData/diagnostics/testsWithAnyBackend",
                    "testData/diagnostics/testsWithStdLib",
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
            // TODO: uncomment after OSIP-1363 is done
            //testClass<AbstractPhasedJvmDiagnosticMultiplatformParsingTest> {
            //    phasedModel(allowKts = false)
            //}
            testClass<AbstractPhasedJvmDiagnosticLightTreeTest> {
                phasedModel(allowKts = false)
            }
            testClass<AbstractPhasedJvmDiagnosticPsiTest> {
                phasedModel(allowKts = true)
            }
        }

        testGroup(testRoot, "compiler/testData/diagnostics/tests/contextSensitiveResolutionUsingExpectedType") {
            testClass<AbstractPhasedJvmDiagnosticPsiWithContextSensitiveEnabledTest> {
                model("ideHint", excludedPattern = CUSTOM_TEST_DATA_EXTENSION_PATTERN)
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

        testGroup(testRoot, testDataRoot = "compiler/testData/codegen") {
            testClass<AbstractFirLightTreeBlackBoxCodegenTest> {
                model("box")
                model("boxJvm")
            }

            testClass<AbstractValhallaBlackBoxSmokeTest> {
                model("box")
                model("boxJvm")
            }

            testClass<AbstractFirLightTreeHeaderModeCodegenTest> {
                model("box")
                model("boxJvm")
            }

            testClass<AbstractFirPsiBlackBoxCodegenTest> {
                model("box")
                model("boxJvm")
            }
            testClass<AbstractJvmLightTreeBlackBoxCodegenWithSeparateKmpCompilationTest> {
                model("box/multiplatform/k2")
                model("boxJvm/multiplatform/k2")
            }

            testClass<AbstractReflectionLegacyImplementationTest> {
                model("box/reflection")
                model("boxJvm/reflection")
            }

            testClass<AbstractReflectionK1MembersImplementationTest> {
                model("box/reflection")
                model("boxJvm/reflection")
            }

            testClass<AbstractReflectionLoadMetadataDirectlyTest> {
                model("box/reflection")
                model("boxJvm/reflection")
            }
        }

        testGroup(testRoot, testDataRoot = "compiler/testData") {
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

            testClass<AbstractFirLightTreeSteppingTest> {
                model("debug/stepping")
            }

            testClass<AbstractFirPsiSteppingTest> {
                model("debug/stepping")
            }

            testClass<AbstractFirLightTreeLocalVariableTest> {
                model("debug/localVariables")
            }

            testClass<AbstractLocalVariableTableTest> {
                model("checkLocalVariablesTable")
            }

            testClass<AbstractFirPsiLocalVariableTest> {
                model("debug/localVariables")
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

            testClass<AbstractWriteSignatureTest> {
                model("writeSignature")
            }

            testClass<AbstractWriteFlagsTest> {
                model("writeFlags")
            }
        }

        testGroup(testRoot, testDataRoot = "compiler/testData") {
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

        testGroup(testRoot, "compiler/tests-spec/testData") {
            testClass<AbstractFirBlackBoxCodegenTestSpec> {
                model(
                    relativeRootPath = "codegen/box",
                    excludeDirs = listOf("helpers", "templates") + detectDirsWithTestsMapFileOnly("codegen/box"),
                )
            }
        }

        testGroup(testRoot, testDataRoot = "compiler/testData/codegen") {
            testClass<AbstractDirectivesValidatorTest> {
                model("box")
                model("boxJvm")
            }

            testClass<AbstractFirBlackBoxCodegenTestWithInlineScopes> {
                val excludedScriptDirs = listOf("script")
                model("box", excludeDirs = excludedScriptDirs)
                model("boxJvm", excludeDirs = excludedScriptDirs)
            }
        }

        testGroup(testRoot, testDataRoot = "compiler/testData") {
            testClass<AbstractCompileJavaAgainstKotlinTest> {
                model("compileJavaAgainstKotlin")
            }

            // ------------- Inline scopes tests duplication -------------

            testClass<AbstractFirBytecodeTextTestWithInlineScopes> {
                model("codegen/bytecodeText")
            }

            testClass<AbstractFirSteppingTestWithInlineScopes> {
                model("debug/stepping")
            }

            testClass<AbstractFirLocalVariableTestWithInlineScopes> {
                // We exclude the 'inlineScopes/newFormatToOld' directory from tests that have inline scopes enabled
                // by default, since we only want to test the scenario where code with inline scopes is inlined by the
                // old inliner with $iv suffixes.
                val inlineScopesNewFormatToOld = listOf("inlineScopes/newFormatToOld")

                model("debug/localVariables", excludeDirs = inlineScopesNewFormatToOld)
            }

            testClass<AbstractFirBlackBoxInlineCodegenTestWithInlineScopes> {
                model("codegen/boxInline", excludeDirs = listOf("multiplatform/k2"))
            }

            this.testClass<AbstractFirBlackBoxCodegenTestWithInlineScopes>("FirBlackBoxModernJdkCodegenTestGeneratedWithInlineScopes") {
                model("codegen/boxModernJdk")
            }
        }
    }
}
