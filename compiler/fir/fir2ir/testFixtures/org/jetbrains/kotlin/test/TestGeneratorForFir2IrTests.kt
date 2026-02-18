/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test

import org.jetbrains.kotlin.generators.dsl.junit5.generateTestGroupSuiteWithJUnit5
import org.jetbrains.kotlin.generators.util.TestGeneratorUtil
import org.jetbrains.kotlin.spec.utils.tasks.detectDirsWithTestsMapFileOnly
import org.jetbrains.kotlin.test.runners.AbstractFirBlackBoxCodegenTestSpec
import org.jetbrains.kotlin.test.runners.AbstractFirLightTreeDiagnosticsTestWithJvmIrBackend
import org.jetbrains.kotlin.test.runners.AbstractFirPsiDiagnosticsTestWithConverter
import org.jetbrains.kotlin.test.runners.AbstractFirPsiDiagnosticsTestWithJvmIrBackend
import org.jetbrains.kotlin.test.runners.codegen.*
import org.jetbrains.kotlin.test.runners.ir.*
import org.jetbrains.kotlin.test.utils.CUSTOM_TEST_DATA_EXTENSION_PATTERN

fun main(args: Array<String>) {
    val mainClassName = TestGeneratorUtil.getMainClassName()
    val testRoot = args[0]
    val excludedCustomTestdataPattern = CUSTOM_TEST_DATA_EXTENSION_PATTERN
    val k1BoxTestDir = listOf("multiplatform/k1")
    val k2BoxTestDir = listOf("multiplatform/k2")
    val excludedScriptDirs = listOf("script")

    generateTestGroupSuiteWithJUnit5(args, mainClassName) {
        testGroup(testRoot, testDataRoot = "compiler/testData") {
            testClass<AbstractFirLightTreeBlackBoxCodegenTest> {
                model("codegen/box", excludeDirs = k1BoxTestDir + excludedScriptDirs)
            }

            testClass<AbstractFirLightTreeHeaderModeCodegenTest> {
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

            testClass<AbstractReflectionLegacyImplementationTest> {
                model("codegen/box/reflection")
            }

            testClass<AbstractNewReflectionFakeOverridesImplementationTest> {
                model("codegen/box/reflection")
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
    }
}

