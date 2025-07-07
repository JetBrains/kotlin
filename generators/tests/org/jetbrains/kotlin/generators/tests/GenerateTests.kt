/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.generators.tests

import org.jetbrains.kotlin.allopen.*
import org.jetbrains.kotlin.assignment.plugin.AbstractAssignmentPluginDiagnosticTest
import org.jetbrains.kotlin.assignment.plugin.AbstractFirLightTreeBlackBoxCodegenTestForAssignmentPlugin
import org.jetbrains.kotlin.assignment.plugin.AbstractFirPsiAssignmentPluginDiagnosticTest
import org.jetbrains.kotlin.assignment.plugin.AbstractIrBlackBoxCodegenTestAssignmentPlugin
import org.jetbrains.kotlin.compiler.plugins.AbstractPluginInteractionFirBlackBoxCodegenTest
import org.jetbrains.kotlin.fir.dataframe.AbstractCompilerFacilityTestForDataFrame
import org.jetbrains.kotlin.fir.dataframe.AbstractDataFrameBlackBoxCodegenTest
import org.jetbrains.kotlin.fir.dataframe.AbstractDataFrameDiagnosticTest
import org.jetbrains.kotlin.generators.generateTestGroupSuiteWithJUnit5
import org.jetbrains.kotlin.generators.impl.generateTestGroupSuite
import org.jetbrains.kotlin.generators.tests.IncrementalTestsGeneratorUtil.Companion.IcTestTypes.PURE_KOTLIN
import org.jetbrains.kotlin.generators.tests.IncrementalTestsGeneratorUtil.Companion.IcTestTypes.WITH_JAVA
import org.jetbrains.kotlin.generators.tests.IncrementalTestsGeneratorUtil.Companion.incrementalJvmTestData
import org.jetbrains.kotlin.generators.util.TestGeneratorUtil
import org.jetbrains.kotlin.incremental.*
import org.jetbrains.kotlin.jvm.abi.AbstractCompareJvmAbiTest
import org.jetbrains.kotlin.jvm.abi.AbstractCompileAgainstJvmAbiTest
import org.jetbrains.kotlin.jvm.abi.AbstractJvmAbiContentTest
import org.jetbrains.kotlin.kapt.cli.test.AbstractArgumentParsingTest
import org.jetbrains.kotlin.kapt.cli.test.AbstractFirKaptToolIntegrationTest
import org.jetbrains.kotlin.kapt.cli.test.AbstractKaptToolIntegrationTest
import org.jetbrains.kotlin.kapt.test.AbstractFirKaptStubConverterTest
import org.jetbrains.kotlin.kapt.test.runners.AbstractIrKotlinKaptContextTest
import org.jetbrains.kotlin.kapt.test.runners.AbstractKaptStubConverterTest
import org.jetbrains.kotlin.lombok.AbstractDiagnosticTestForLombok
import org.jetbrains.kotlin.lombok.AbstractFirLightTreeBlackBoxCodegenTestForLombok
import org.jetbrains.kotlin.lombok.AbstractFirPsiDiagnosticTestForLombok
import org.jetbrains.kotlin.lombok.AbstractIrBlackBoxCodegenTestForLombok
import org.jetbrains.kotlin.noarg.*
import org.jetbrains.kotlin.parcelize.test.runners.*
import org.jetbrains.kotlin.plugin.sandbox.*
import org.jetbrains.kotlin.powerassert.AbstractFirLightTreeBlackBoxCodegenTestForPowerAssert
import org.jetbrains.kotlin.powerassert.AbstractIrBlackBoxCodegenTestForPowerAssert
import org.jetbrains.kotlin.samWithReceiver.*
import org.jetbrains.kotlin.scripting.test.*
import org.jetbrains.kotlin.test.TargetBackend
import org.jetbrains.kotlin.test.utils.CUSTOM_TEST_DATA_EXTENSION_PATTERN
import org.jetbrains.kotlinx.atomicfu.incremental.AbstractIncrementalK2JVMWithAtomicfuRunnerTest
import org.jetbrains.kotlinx.atomicfu.runners.*

private class ExcludePattern {
    companion object {
        private const val MEMBER_ALIAS = "(^removeMemberTypeAlias)|(^addMemberTypeAlias)"

        private const val ALL_EXPECT = "(^.*Expect.*)"

        internal val forK2 = listOf(
            ALL_EXPECT, // KT-63125 - Partially related to single-module expect-actual tests, but regexp is really wide
            MEMBER_ALIAS, // KT-55195 - Invalid for K2
        ).joinToString("|")
    }
}

fun main(args: Array<String>) {
    System.setProperty("java.awt.headless", "true")
    generateTestGroupSuite(args) {
        testGroup("compiler/incremental-compilation-impl/tests-gen", "jps/jps-plugin/testData") {
            testClass<AbstractIncrementalK1JvmCompilerRunnerTest>(
                init = incrementalJvmTestData(
                    targetBackend = TargetBackend.JVM_IR,
                    folderToExcludePatternMap = mapOf(
                        PURE_KOTLIN to ".*SinceK2",
                        WITH_JAVA to "(^javaToKotlin)|(^javaToKotlinAndBack)|(^kotlinToJava)|(^packageFileAdded)|(^changeNotUsedSignature)" // KT-56681
                    )
                )
            )

            // K2
            testClass<AbstractIncrementalK2JvmCompilerRunnerTest>(
                init = incrementalJvmTestData(
                    TargetBackend.JVM_IR,
                    folderToExcludePatternMap = mapOf(
                        PURE_KOTLIN to ExcludePattern.forK2
                    )
                )
            )

            testClass<AbstractIncrementalK2FirICJvmCompilerRunnerTest>(
                init = incrementalJvmTestData(
                    TargetBackend.JVM_IR,
                    folderToExcludePatternMap = mapOf(
                        PURE_KOTLIN to ExcludePattern.forK2,
                        WITH_JAVA to "^classToPackageFacade" // KT-56698
                    )
                )
            )
            testClass<AbstractIncrementalK2PsiJvmCompilerRunnerTest>(
                init = incrementalJvmTestData(
                    TargetBackend.JVM_IR,
                    folderToExcludePatternMap = mapOf(
                        PURE_KOTLIN to ExcludePattern.forK2
                    )
                )
            )

            testClass<AbstractIncrementalK1JsKlibCompilerRunnerTest> {
                // IC of sealed interfaces are not supported in JS
                modelForDirectoryBasedTest("incremental", "pureKotlin", extension = null, recursive = false, excludedPattern = "(^sealed.*)|(.*SinceK2)")
                modelForDirectoryBasedTest("incremental", "classHierarchyAffected", extension = null, recursive = false)
                model("incremental/js", extension = null, excludeParentDirs = true)
            }

            testClass<AbstractIncrementalK1JsKlibMultiModuleCompilerRunnerTest> {
                modelForDirectoryBasedTest("incremental/multiModule", "common", extension = null, excludeParentDirs = true)
            }

            testClass<AbstractIncrementalK2JsKlibMultiModuleCompilerRunnerTest> {
                modelForDirectoryBasedTest("incremental/multiModule", "common", extension = null, excludeParentDirs = true)
            }

            testClass<AbstractIncrementalK1JsKlibCompilerWithScopeExpansionRunnerTest> {
                // IC of sealed interfaces are not supported in JS
                modelForDirectoryBasedTest("incremental", "pureKotlin", extension = null, recursive = false, excludedPattern = "(^sealed.*)|(.*SinceK2)")
                modelForDirectoryBasedTest("incremental", "classHierarchyAffected", extension = null, recursive = false)
                modelForDirectoryBasedTest("incremental", "js", extension = null, excludeParentDirs = true)
                modelForDirectoryBasedTest("incremental", "scopeExpansion", extension = null, excludeParentDirs = true)
            }

            // TODO: https://youtrack.jetbrains.com/issue/KT-61602/JS-K2-ICL-Fix-muted-tests
            testClass<AbstractIncrementalK2JsKlibCompilerWithScopeExpansionRunnerTest> {
                // IC of sealed interfaces are not supported in JS
                modelForDirectoryBasedTest(
                    "incremental", "pureKotlin", extension = null, recursive = false,
                    // TODO: 'fileWithConstantRemoved' should be fixed in https://youtrack.jetbrains.com/issue/KT-58824
                    excludedPattern = "^(sealed.*|fileWithConstantRemoved|propertyRedeclaration|funRedeclaration|funVsConstructorOverloadConflict)"
                )
                modelForDirectoryBasedTest(
                    "incremental", "classHierarchyAffected", extension = null, recursive = false,
                    excludedPattern = "secondaryConstructorAdded"
                )
                modelForDirectoryBasedTest("incremental", "js", extension = null, excludeParentDirs = true)
            }

            testClass<AbstractIncrementalK1JsKlibCompilerRunnerWithFriendModulesDisabledTest> {
                modelForDirectoryBasedTest("incremental/js", "friendsModuleDisabled", extension = null, recursive = false)
            }

            testClass<AbstractIncrementalMultiplatformJvmCompilerRunnerTest> {
                modelForDirectoryBasedTest("incremental/mpp", "allPlatforms", extension = null, excludeParentDirs = true)
                modelForDirectoryBasedTest("incremental/mpp", "jvmOnlyK1", extension = null, excludeParentDirs = true)
            }
            testClass<AbstractIncrementalK1JsKlibMultiplatformJsCompilerRunnerTest> {
                modelForDirectoryBasedTest("incremental/mpp", "allPlatforms", extension = null, excludeParentDirs = true)
            }
            //TODO: write a proper k2 multiplatform test runner KT-63183
        }

        testGroup("plugins/jvm-abi-gen/tests-gen", "plugins/jvm-abi-gen/testData") {
            testClass<AbstractCompareJvmAbiTest> {
                model("compare", recursive = false, extension = null, targetBackend = TargetBackend.JVM_IR)
            }

            testClass<AbstractJvmAbiContentTest> {
                model("content", recursive = false, extension = null, targetBackend = TargetBackend.JVM_IR)
            }

            testClass<AbstractCompileAgainstJvmAbiTest> {
                model("compile", recursive = false, extension = null, targetBackend = TargetBackend.JVM_IR)
            }
        }

        testGroup("plugins/sam-with-receiver/tests-gen", "plugins/sam-with-receiver/testData") {
            testClass<AbstractSamWithReceiverScriptTest> {
                model("script", extension = "kts")
            }

            testClass<AbstractSamWithReceiverScriptNewDefTest> {
                model("script", extension = "kts")
            }
        }

        testGroup("plugins/plugin-sandbox/plugin-sandbox-ic-test/tests-gen", "plugins/plugin-sandbox/plugin-sandbox-ic-test/testData") {
            testClass<AbstractIncrementalK2JvmWithPluginCompilerRunnerTest> {
                model("pureKotlin", extension = null, recursive = false, targetBackend = TargetBackend.JVM_IR)
            }
        }

        testGroup("plugins/atomicfu/atomicfu-compiler/tests-gen", "plugins/atomicfu/atomicfu-compiler/testData/") {
            testClass<AbstractIncrementalK2JVMWithAtomicfuRunnerTest> {
                model("projects/", extension = null, recursive = false, targetBackend = TargetBackend.JVM_IR)
            }
        }
    }

    generateTestGroupSuiteWithJUnit5 {
        val excludedFirTestdataPattern = TestGeneratorUtil.KT_OR_KTS_WITH_FIR_PREFIX

        testGroup("plugins/parcelize/parcelize-compiler/tests-gen", "plugins/parcelize/parcelize-compiler/testData") {
            testClass<AbstractParcelizeIrBoxTest> {
                model("box")
            }

            testClass<AbstractParcelizeFirLightTreeBoxTest> {
                model("box")
            }

            testClass<AbstractParcelizeIrBytecodeListingTest> {
                model("codegen")
            }

            testClass<AbstractFirParcelizeBytecodeListingTest> {
                model("codegen")
            }

            testClass<AbstractParcelizeDiagnosticTest> {
                model("diagnostics", excludedPattern = excludedFirTestdataPattern)
            }

            testClass<AbstractFirPsiParcelizeDiagnosticTest> {
                model("diagnostics", excludedPattern = excludedFirTestdataPattern)
            }
        }

        testGroup("plugins/plugin-sandbox/tests-gen", "plugins/plugin-sandbox/testData") {
            testClass<AbstractFirPsiPluginDiagnosticTest> {
                model("diagnostics", excludedPattern = CUSTOM_TEST_DATA_EXTENSION_PATTERN)
            }

            testClass<AbstractFirLightTreePluginBlackBoxCodegenTest> {
                model("box", excludedPattern = CUSTOM_TEST_DATA_EXTENSION_PATTERN)
            }

            testClass<AbstractFirLoadK2CompiledWithPluginJvmKotlinTest> {
                model("firLoadK2Compiled")
            }

            testClass<AbstractFirLoadK2CompiledWithPluginJsKotlinTest> {
                model("firLoadK2Compiled")
            }

            testClass<AbstractFirMetadataPluginSandboxTest> {
                model("metadata")
            }
        }

        testGroup(
            "plugins/atomicfu/atomicfu-compiler/tests-gen",
            "plugins/atomicfu/atomicfu-compiler/testData",
            testRunnerMethodName = "runTest0"
        ) {
            testClass<AbstractAtomicfuJsIrTest> {
                model(relativeRootPath = "box/", excludeDirs = listOf("context_parameters"))
            }

            testClass<AbstractAtomicfuJsFirTest> {
                model("box/")
            }

            testClass<AbstractAtomicfuJsFirWithInlinedFunInKlibTest> {
                model("box/")
            }
        }

        testGroup(
            "plugins/atomicfu/atomicfu-compiler/tests-gen",
            "plugins/atomicfu/atomicfu-compiler/testData",
            testRunnerMethodName = "runTest0"
        ) {
            testClass<AbstractAtomicfuFirCheckerTest> {
                model("diagnostics/")
            }

            testClass<AbstractAtomicfuJvmIrTest> {
                model(relativeRootPath = "box/", excludeDirs = listOf("context_parameters"))
            }

            testClass<AbstractAtomicfuJvmFirLightTreeTest> {
                model("box/")
            }
        }

        testGroup("plugins/allopen/tests-gen", "plugins/allopen/testData") {
            testClass<AbstractIrBytecodeListingTestForAllOpen> {
                model("bytecodeListing", excludedPattern = excludedFirTestdataPattern)
            }
            testClass<AbstractFirPsiBytecodeListingTestForAllOpen> {
                model("bytecodeListing", excludedPattern = excludedFirTestdataPattern)
            }
            testClass<AbstractDiagnosticTestForAllOpenBase> {
                model("diagnostics", excludedPattern = excludedFirTestdataPattern)
            }
            testClass<AbstractFirLightTreeDiagnosticTestForAllOpen> {
                model("diagnostics", excludedPattern = excludedFirTestdataPattern)
            }
            testClass<AbstractFirPsiDiagnosticTestForAllOpen> {
                model("diagnostics", excludedPattern = excludedFirTestdataPattern)
            }
        }

        testGroup("plugins/noarg/tests-gen", "plugins/noarg/testData") {
            testClass<AbstractDiagnosticsTestForNoArg> {
                model("diagnostics", excludedPattern = excludedFirTestdataPattern)
            }
            testClass<AbstractFirPsiDiagnosticsTestForNoArg> {
                model("diagnostics", excludedPattern = excludedFirTestdataPattern)
            }
            testClass<AbstractIrBytecodeListingTestForNoArg> {
                model("bytecodeListing", excludedPattern = excludedFirTestdataPattern)
            }
            testClass<AbstractFirLightTreeBytecodeListingTestForNoArg> {
                model("bytecodeListing", excludedPattern = excludedFirTestdataPattern)
            }
            testClass<AbstractIrBlackBoxCodegenTestForNoArg> {
                model("box")
            }
            testClass<AbstractFirLightTreeBlackBoxCodegenTestForNoArg> {
                model("box")
            }
        }

        testGroup("plugins/lombok/tests-gen", "plugins/lombok/testData") {
            testClass<AbstractIrBlackBoxCodegenTestForLombok> {
                model("box")
            }
            testClass<AbstractFirLightTreeBlackBoxCodegenTestForLombok> {
                model("box")
            }
            testClass<AbstractDiagnosticTestForLombok> {
                model("diagnostics/k1+k2", excludedPattern = excludedFirTestdataPattern)
            }
            testClass<AbstractFirPsiDiagnosticTestForLombok> {
                model("diagnostics")
            }
        }

        testGroup("plugins/power-assert/tests-gen", "plugins/power-assert/testData") {
            testClass<AbstractIrBlackBoxCodegenTestForPowerAssert> {
                model("codegen", excludedPattern = excludedFirTestdataPattern)
            }
            testClass<AbstractFirLightTreeBlackBoxCodegenTestForPowerAssert> {
                model("codegen", excludedPattern = excludedFirTestdataPattern)
            }
        }

        testGroup("plugins/sam-with-receiver/tests-gen", "plugins/sam-with-receiver/testData") {
            testClass<AbstractSamWithReceiverTest> {
                model("diagnostics", excludedPattern = excludedFirTestdataPattern)
            }
            testClass<AbstractFirPsiSamWithReceiverDiagnosticTest> {
                model("diagnostics", excludedPattern = excludedFirTestdataPattern)
            }
            testClass<AbstractIrBlackBoxCodegenTestForSamWithReceiver> {
                model("codegen", excludedPattern = excludedFirTestdataPattern)
            }
            testClass<AbstractFirLightTreeBlackBoxCodegenTestForSamWithReceiver> {
                model("codegen", excludedPattern = excludedFirTestdataPattern)
            }
        }

        testGroup("plugins/kapt/kapt-cli/tests-gen", "plugins/kapt/kapt-cli/testData") {
            testClass<AbstractArgumentParsingTest> {
                model("argumentParsing", extension = "txt")
            }
            testClass<AbstractKaptToolIntegrationTest> {
                model("integration", recursive = false, extension = null)
            }
            testClass<AbstractFirKaptToolIntegrationTest> {
                model("integration-kapt4", recursive = false, extension = null)
            }
        }

        testGroup("plugins/kapt/kapt-compiler/tests-gen", "plugins/kapt/kapt-compiler/testData") {
            testClass<AbstractIrKotlinKaptContextTest> {
                model("kotlinRunner")
            }
            testClass<AbstractKaptStubConverterTest> {
                model("converter")
            }
            testClass<AbstractFirKaptStubConverterTest> {
                model("converter")
            }
        }

        testGroup("plugins/scripting/scripting-tests/tests-gen", "plugins/scripting/scripting-tests") {
            testClass<AbstractScriptWithCustomDefDiagnosticsTestBase> {
                model("testData/diagnostics/testScripts", extension = "kts")
            }
        }

        testGroup("plugins/scripting/scripting-tests/tests-gen", "plugins/scripting/scripting-tests") {
            testClass<AbstractScriptWithCustomDefBlackBoxCodegenTest> {
                model("testData/codegen/testScripts", extension = "kts")
            }
        }

        testGroup("plugins/scripting/scripting-tests/tests-gen", "plugins/scripting/scripting-tests") {
            testClass<AbstractReplWithTestExtensionsDiagnosticsTest> {
                model("testData/diagnostics/repl", extension = "kts")
            }
        }

        testGroup("plugins/scripting/scripting-tests/tests-gen", "plugins/scripting/scripting-tests") {
            testClass<AbstractReplViaApiDiagnosticsTest> {
                model("testData/diagnostics/repl", extension = "kts")
            }
        }

        testGroup("plugins/scripting/scripting-tests/tests-gen", "plugins/scripting/scripting-tests") {
            testClass<AbstractReplWithTestExtensionsCodegenTest> {
                model("testData/codegen/repl", extension = "kts")
            }
        }

        testGroup("plugins/scripting/scripting-tests/tests-gen", "plugins/scripting/scripting-tests") {
            testClass<AbstractReplViaApiEvaluationTest> {
                model("testData/codegen/repl", extension = "kts")
            }
        }

        testGroup("plugins/assign-plugin/tests-gen", "plugins/assign-plugin/testData") {
            testClass<AbstractAssignmentPluginDiagnosticTest> {
                model("diagnostics", excludedPattern = excludedFirTestdataPattern)
            }
            testClass<AbstractFirPsiAssignmentPluginDiagnosticTest> {
                model("diagnostics", excludedPattern = excludedFirTestdataPattern)
            }
            testClass<AbstractIrBlackBoxCodegenTestAssignmentPlugin> {
                model("codegen", excludedPattern = excludedFirTestdataPattern)
            }
            testClass<AbstractFirLightTreeBlackBoxCodegenTestForAssignmentPlugin> {
                model("codegen", excludedPattern = excludedFirTestdataPattern)
            }
        }

        testGroup("plugins/plugins-interactions-testing/tests-gen", "plugins/plugins-interactions-testing/testData") {
            testClass<AbstractPluginInteractionFirBlackBoxCodegenTest> {
                model("box", excludedPattern = excludedFirTestdataPattern)
            }
        }

        testGroup("plugins/kotlin-dataframe/tests-gen", "plugins/kotlin-dataframe/testData") {
            testClass<AbstractDataFrameDiagnosticTest> {
                model("diagnostics")
            }

            testClass<AbstractDataFrameBlackBoxCodegenTest> {
                model("box")
            }

            testClass<AbstractCompilerFacilityTestForDataFrame> {
                model("compilerFacility")
            }
        }
    }
}
