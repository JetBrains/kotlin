/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.generators.tests

import org.jetbrains.kotlin.allopen.*
import org.jetbrains.kotlin.android.parcel.AbstractParcelBoxTest
import org.jetbrains.kotlin.android.parcel.AbstractParcelBytecodeListingTest
import org.jetbrains.kotlin.android.parcel.AbstractParcelIrBoxTest
import org.jetbrains.kotlin.android.parcel.AbstractParcelIrBytecodeListingTest
import org.jetbrains.kotlin.android.synthetic.test.AbstractAndroidBoxTest
import org.jetbrains.kotlin.android.synthetic.test.AbstractAndroidBytecodeShapeTest
import org.jetbrains.kotlin.android.synthetic.test.AbstractAndroidIrBoxTest
import org.jetbrains.kotlin.android.synthetic.test.AbstractAndroidSyntheticPropertyDescriptorTest
import org.jetbrains.kotlin.assignment.plugin.AbstractAssignmentPluginDiagnosticTest
import org.jetbrains.kotlin.assignment.plugin.AbstractFirLightTreeBlackBoxCodegenTestForAssignmentPlugin
import org.jetbrains.kotlin.assignment.plugin.AbstractFirPsiAssignmentPluginDiagnosticTest
import org.jetbrains.kotlin.assignment.plugin.AbstractIrBlackBoxCodegenTestAssignmentPlugin
import org.jetbrains.kotlin.fir.plugin.runners.AbstractFirLightTreePluginBlackBoxCodegenTest
import org.jetbrains.kotlin.fir.plugin.runners.AbstractFirLoadK2CompiledWithPluginJsKotlinTest
import org.jetbrains.kotlin.fir.plugin.runners.AbstractFirLoadK2CompiledWithPluginJvmKotlinTest
import org.jetbrains.kotlin.fir.plugin.runners.AbstractFirPsiPluginDiagnosticTest
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
import org.jetbrains.kotlin.kapt.cli.test.AbstractKapt4ToolIntegrationTest
import org.jetbrains.kotlin.kapt.cli.test.AbstractKaptToolIntegrationTest
import org.jetbrains.kotlin.kapt3.test.runners.AbstractIrClassFileToSourceStubConverterTest
import org.jetbrains.kotlin.kapt3.test.runners.AbstractIrKotlinKaptContextTest
import org.jetbrains.kotlin.kapt4.AbstractKotlinKapt4ContextTest
import org.jetbrains.kotlin.lombok.*
import org.jetbrains.kotlin.noarg.*
import org.jetbrains.kotlin.parcelize.test.runners.*
import org.jetbrains.kotlin.powerassert.AbstractFirLightTreeBlackBoxCodegenTestForPowerAssert
import org.jetbrains.kotlin.powerassert.AbstractIrBlackBoxCodegenTestForPowerAssert
import org.jetbrains.kotlin.samWithReceiver.*
import org.jetbrains.kotlin.test.TargetBackend
import org.jetbrains.kotlinx.atomicfu.AbstractAtomicfuJsIrTest
import org.jetbrains.kotlinx.atomicfu.AbstractAtomicfuJvmIrTest


private class ExcludePattern {
    companion object {
        private const val MEMBER_ALIAS = "(^removeMemberTypeAlias)|(^addMemberTypeAlias)"

        private const val ALL_EXPECT = "(^.*Expect.*)"
        private const val COMPANION_CONSTANT = "(^companionConstantChanged)"

        internal val forK2 = listOf(
            ALL_EXPECT, // KT-63125 - Partially related to single-module expect-actual tests, but regexp is really wide
            MEMBER_ALIAS, // KT-55195 - Invalid for K2
            COMPANION_CONSTANT // KT-56242 - Work in progress
        ).joinToString("|")
    }
}

fun main(args: Array<String>) {
    System.setProperty("java.awt.headless", "true")
    generateTestGroupSuite(args) {
        testGroup("compiler/incremental-compilation-impl/test", "jps/jps-plugin/testData") {
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
                model("incremental/pureKotlin", extension = null, recursive = false, excludedPattern = "(^sealed.*)|(.*SinceK2)")
                model("incremental/classHierarchyAffected", extension = null, recursive = false)
                model("incremental/js", extension = null, excludeParentDirs = true)
            }

            testClass<AbstractIncrementalK1JsKlibMultiModuleCompilerRunnerTest> {
                model("incremental/multiModule/common", extension = null, excludeParentDirs = true)
            }

            testClass<AbstractIncrementalK2JsKlibMultiModuleCompilerRunnerTest> {
                model("incremental/multiModule/common", extension = null, excludeParentDirs = true)
            }

            testClass<AbstractIncrementalK1JsKlibCompilerWithScopeExpansionRunnerTest> {
                // IC of sealed interfaces are not supported in JS
                model("incremental/pureKotlin", extension = null, recursive = false, excludedPattern = "^sealed.*")
                model("incremental/classHierarchyAffected", extension = null, recursive = false)
                model("incremental/js", extension = null, excludeParentDirs = true)
                model("incremental/scopeExpansion", extension = null, excludeParentDirs = true)
            }

            // TODO: https://youtrack.jetbrains.com/issue/KT-61602/JS-K2-ICL-Fix-muted-tests
            testClass<AbstractIncrementalK2JsKlibCompilerWithScopeExpansionRunnerTest> {
                // IC of sealed interfaces are not supported in JS
                model(
                    "incremental/pureKotlin", extension = null, recursive = false,
                    // TODO: 'fileWithConstantRemoved' should be fixed in https://youtrack.jetbrains.com/issue/KT-58824
                    excludedPattern = "^(sealed.*|fileWithConstantRemoved|propertyRedeclaration|funRedeclaration|funVsConstructorOverloadConflict)"
                )
                model(
                    "incremental/classHierarchyAffected", extension = null, recursive = false,
                    excludedPattern = "secondaryConstructorAdded"
                )
                model("incremental/js", extension = null, excludeParentDirs = true)
            }

            testClass<AbstractIncrementalK1JsKlibCompilerRunnerWithFriendModulesDisabledTest> {
                model("incremental/js/friendsModuleDisabled", extension = null, recursive = false)
            }

            testClass<AbstractIncrementalMultiplatformJvmCompilerRunnerTest> {
                model("incremental/mpp/allPlatforms", extension = null, excludeParentDirs = true)
                model("incremental/mpp/jvmOnlyK1", extension = null, excludeParentDirs = true)
            }
            testClass<AbstractIncrementalK1JsKlibMultiplatformJsCompilerRunnerTest> {
                model("incremental/mpp/allPlatforms", extension = null, excludeParentDirs = true)
            }
            //TODO: write a proper k2 multiplatform test runner KT-63183
        }

        testGroup(
            "plugins/android-extensions/android-extensions-compiler/test",
            "plugins/android-extensions/android-extensions-compiler/testData"
        ) {
            testClass<AbstractAndroidSyntheticPropertyDescriptorTest> {
                model("descriptors", recursive = false, extension = null)
            }

            testClass<AbstractAndroidBoxTest> {
                model("codegen/android", recursive = false, extension = null, testMethod = "doCompileAgainstAndroidSdkTest")
                model("codegen/android", recursive = false, extension = null, testMethod = "doFakeInvocationTest", testClassName = "Invoke")
            }

            testClass<AbstractAndroidIrBoxTest> {
                model(
                    "codegen/android", recursive = false, extension = null, testMethod = "doCompileAgainstAndroidSdkTest",
                    targetBackend = TargetBackend.JVM_IR
                )
                model(
                    "codegen/android", recursive = false, extension = null, testMethod = "doFakeInvocationTest", testClassName = "Invoke",
                    targetBackend = TargetBackend.JVM_IR
                )
            }

            testClass<AbstractAndroidBytecodeShapeTest> {
                model("codegen/bytecodeShape", recursive = false, extension = null)
            }

            testClass<AbstractParcelBoxTest> {
                model("parcel/box", targetBackend = TargetBackend.JVM)
            }

            testClass<AbstractParcelIrBoxTest> {
                model("parcel/box", targetBackend = TargetBackend.JVM_IR)
            }

            testClass<AbstractParcelBytecodeListingTest> {
                model("parcel/codegen", targetBackend = TargetBackend.JVM)
            }

            testClass<AbstractParcelIrBytecodeListingTest> {
                model("parcel/codegen", targetBackend = TargetBackend.JVM_IR)
            }
        }

        testGroup("plugins/jvm-abi-gen/test", "plugins/jvm-abi-gen/testData") {
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

        testGroup("plugins/fir-plugin-prototype/fir-plugin-ic-test/tests-gen", "plugins/fir-plugin-prototype/fir-plugin-ic-test/testData") {
            testClass<AbstractIncrementalK2JvmWithPluginCompilerRunnerTest> {
                model("pureKotlin", extension = null, recursive = false, targetBackend = TargetBackend.JVM_IR)
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

            testClass<AbstractParcelizeDiagnosticTest> {
                model("diagnostics", excludedPattern = excludedFirTestdataPattern)
            }

            testClass<AbstractFirPsiParcelizeDiagnosticTest> {
                model("diagnostics", excludedPattern = excludedFirTestdataPattern)
            }
        }

        testGroup("plugins/fir-plugin-prototype/tests-gen", "plugins/fir-plugin-prototype/testData") {
            testClass<AbstractFirPsiPluginDiagnosticTest> {
                model("diagnostics")
            }

            testClass<AbstractFirLightTreePluginBlackBoxCodegenTest> {
                model("box")
            }

            testClass<AbstractFirLoadK2CompiledWithPluginJvmKotlinTest> {
                model("firLoadK2Compiled")
            }

            testClass<AbstractFirLoadK2CompiledWithPluginJsKotlinTest> {
                model("firLoadK2Compiled")
            }
        }

        testGroup(
            "plugins/atomicfu/atomicfu-compiler/test",
            "plugins/atomicfu/atomicfu-compiler/testData",
            testRunnerMethodName = "runTest0"
        ) {
            testClass<AbstractAtomicfuJsIrTest> {
                model("box/")
            }
        }

        testGroup(
            "plugins/atomicfu/atomicfu-compiler/test",
            "plugins/atomicfu/atomicfu-compiler/testData",
            testRunnerMethodName = "runTest0"
        ) {
            testClass<AbstractAtomicfuJvmIrTest> {
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
            testClass<AbstractBlackBoxCodegenTestForNoArg> {
                model("box")
            }
            testClass<AbstractIrBlackBoxCodegenTestForNoArg> {
                model("box")
            }
            testClass<AbstractFirLightTreeBlackBoxCodegenTestForNoArg> {
                model("box")
            }
        }

        testGroup("plugins/lombok/tests-gen", "plugins/lombok/testData") {
            testClass<AbstractBlackBoxCodegenTestForLombok> {
                model("box")
            }
            testClass<AbstractIrBlackBoxCodegenTestForLombok> {
                model("box")
            }
            testClass<AbstractFirLightTreeBlackBoxCodegenTestForLombok> {
                model("box")
            }
            testClass<AbstractDiagnosticTestForLombok> {
                model("diagnostics", excludedPattern = excludedFirTestdataPattern)
            }
            testClass<AbstractFirPsiDiagnosticTestForLombok> {
                model("diagnostics", excludedPattern = excludedFirTestdataPattern)
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

        testGroup("plugins/kapt3/kapt3-cli/tests-gen", "plugins/kapt3/kapt3-cli/testData") {
            testClass<AbstractArgumentParsingTest> {
                model("argumentParsing", extension = "txt")
            }
            testClass<AbstractKaptToolIntegrationTest> {
                model("integration", recursive = false, extension = null)
            }
            testClass<AbstractKapt4ToolIntegrationTest> {
                model("integration-kapt4", recursive = false, extension = null)
            }
        }

        testGroup("plugins/kapt3/kapt3-compiler/tests-gen", "plugins/kapt3/kapt3-compiler/testData") {
            testClass<AbstractIrKotlinKaptContextTest> {
                model("kotlinRunner")
            }

            testClass<AbstractIrClassFileToSourceStubConverterTest> {
                model("converter")
            }
        }
        testGroup("plugins/kapt4/tests-gen", "plugins/kapt4/") {
            testClass<AbstractKotlinKapt4ContextTest> {
                model("../kapt3/kapt3-compiler/testData/converter")
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
    }
}
