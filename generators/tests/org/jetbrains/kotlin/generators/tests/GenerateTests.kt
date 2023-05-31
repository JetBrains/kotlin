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
import org.jetbrains.kotlin.fir.plugin.runners.AbstractFirLoadK2CompiledWithPluginJvmKotlinTest
import org.jetbrains.kotlin.fir.plugin.runners.AbstractFirPsiPluginDiagnosticTest
import org.jetbrains.kotlin.generators.generateTestGroupSuiteWithJUnit5
import org.jetbrains.kotlin.generators.impl.generateTestGroupSuite
import org.jetbrains.kotlin.generators.model.annotation
import org.jetbrains.kotlin.generators.tests.IncrementalTestsGeneratorUtil.Companion.IcTestTypes.PURE_KOTLIN
import org.jetbrains.kotlin.generators.tests.IncrementalTestsGeneratorUtil.Companion.IcTestTypes.WITH_JAVA
import org.jetbrains.kotlin.generators.tests.IncrementalTestsGeneratorUtil.Companion.incrementalJvmTestData
import org.jetbrains.kotlin.incremental.*
import org.jetbrains.kotlin.jvm.abi.AbstractCompareJvmAbiTest
import org.jetbrains.kotlin.jvm.abi.AbstractCompileAgainstJvmAbiTest
import org.jetbrains.kotlin.jvm.abi.AbstractJvmAbiContentTest
import org.jetbrains.kotlin.kapt.cli.test.AbstractArgumentParsingTest
import org.jetbrains.kotlin.kapt.cli.test.AbstractKaptToolIntegrationTest
import org.jetbrains.kotlin.kapt3.test.runners.AbstractClassFileToSourceStubConverterTest
import org.jetbrains.kotlin.kapt3.test.runners.AbstractIrClassFileToSourceStubConverterTest
import org.jetbrains.kotlin.kapt3.test.runners.AbstractIrKotlinKaptContextTest
import org.jetbrains.kotlin.kapt3.test.runners.AbstractKotlinKaptContextTest
import org.jetbrains.kotlin.lombok.*
import org.jetbrains.kotlin.noarg.*
import org.jetbrains.kotlin.parcelize.test.runners.*
import org.jetbrains.kotlin.samWithReceiver.*
import org.jetbrains.kotlin.test.TargetBackend
import org.jetbrains.kotlinx.atomicfu.AbstractAtomicfuJsIrTest
import org.jetbrains.kotlinx.atomicfu.AbstractAtomicfuJvmIrTest
import org.junit.jupiter.api.Tag

fun main(args: Array<String>) {
    System.setProperty("java.awt.headless", "true")
    generateTestGroupSuite(args) {
        testGroup("compiler/incremental-compilation-impl/test", "jps/jps-plugin/testData") {
            testClass<AbstractIncrementalJvmCompilerRunnerTest>(
                init = incrementalJvmTestData(
                    targetBackend = TargetBackend.JVM_IR,
                    folderToExcludePatternMap = mapOf(
                        PURE_KOTLIN to ".*SinceK2",
                        WITH_JAVA to "(^javaToKotlin)|(^javaToKotlinAndBack)|(^kotlinToJava)|(^packageFileAdded)|(^changeNotUsedSignature)" // KT-56681
                    )
                )
            )

            // K2
            testClass<AbstractIncrementalFirJvmCompilerRunnerTest>(
                init = incrementalJvmTestData(
                    TargetBackend.JVM_IR,
                    folderToExcludePatternMap = mapOf(
                        PURE_KOTLIN to "(^.*Expect.*)"
                                + "|(^removeMemberTypeAlias)|(^addMemberTypeAlias)" //KT-55195
                                + "|(^companionConstantChanged)" //KT-56242
                    )
                )
            )

            testClass<AbstractIncrementalFirICLightTreeJvmCompilerRunnerTest>(
                init = incrementalJvmTestData(
                    TargetBackend.JVM_IR,
                    folderToExcludePatternMap = mapOf(
                        PURE_KOTLIN to "(^.*Expect.*)"
                                + "|(^removeMemberTypeAlias)|(^addMemberTypeAlias)" //KT-55195
                                + "|(^companionConstantChanged)", //KT-56242
                        WITH_JAVA to "^classToPackageFacade" // KT-56698
                    )
                )
            )
            testClass<AbstractIncrementalFirLightTreeJvmCompilerRunnerTest>(
                init = incrementalJvmTestData(
                    TargetBackend.JVM_IR,
                    folderToExcludePatternMap = mapOf(
                        PURE_KOTLIN to "(^.*Expect.*)"
                                + "|(^removeMemberTypeAlias)|(^addMemberTypeAlias)" //KT-55195
                                + "|(^companionConstantChanged)" //KT-56242
                    )
                )
            )

            testClass<AbstractIncrementalJsCompilerRunnerTest> {
                model("incremental/pureKotlin", extension = null, recursive = false, excludedPattern = ".*SinceK2")
                model("incremental/classHierarchyAffected", extension = null, recursive = false)
                model("incremental/js", extension = null, excludeParentDirs = true)
            }

            testClass<AbstractIncrementalJsKlibCompilerRunnerTest>() {
                // IC of sealed interfaces are not supported in JS
                model("incremental/pureKotlin", extension = null, recursive = false, excludedPattern = "(^sealed.*)|(.*SinceK2)")
                model("incremental/classHierarchyAffected", extension = null, recursive = false)
                model("incremental/js", extension = null, excludeParentDirs = true)
            }

            testClass<AbstractIncrementalMultiModuleJsCompilerRunnerTest> {
                model("incremental/multiModule/common", extension = null, excludeParentDirs = true)
            }

            testClass<AbstractIncrementalMultiModuleJsKlibCompilerRunnerTest> {
                model("incremental/multiModule/common", extension = null, excludeParentDirs = true)
            }

            testClass<AbstractIncrementalJsCompilerRunnerWithMetadataOnlyTest> {
                model("incremental/pureKotlin", extension = null, recursive = false, excludedPattern = ".*SinceK2")
                model("incremental/classHierarchyAffected", extension = null, recursive = false)
                model("incremental/js", extension = null, excludeParentDirs = true)
            }

            testClass<AbstractIncrementalJsKlibCompilerWithScopeExpansionRunnerTest> {
                // IC of sealed interfaces are not supported in JS
                model("incremental/pureKotlin", extension = null, recursive = false, excludedPattern = "^sealed.*")
                model("incremental/classHierarchyAffected", extension = null, recursive = false)
                model("incremental/js", extension = null, excludeParentDirs = true)
                model("incremental/scopeExpansion", extension = null, excludeParentDirs = true)
            }

            testClass<AbstractIncrementalJsFirKlibCompilerWithScopeExpansionRunnerTest> {
                // IC of sealed interfaces are not supported in JS
                model("incremental/pureKotlin", extension = null, recursive = false,
                    // TODO: 'fileWithConstantRemoved' should be fixed in https://youtrack.jetbrains.com/issue/KT-58824
                    excludedPattern = "^(sealed|fileWithConstantRemoved).*")
                model("incremental/classHierarchyAffected", extension = null, recursive = false)
                model("incremental/js", extension = null, excludeParentDirs = true)
                model("incremental/scopeExpansion", extension = null, excludeParentDirs = true)
            }

            testClass<AbstractIncrementalJsCompilerRunnerWithFriendModulesDisabledTest> {
                model("incremental/js/friendsModuleDisabled", extension = null, recursive = false)
            }

            testClass<AbstractIncrementalMultiplatformJvmCompilerRunnerTest> {
                model("incremental/mpp/allPlatforms", extension = null, excludeParentDirs = true)
                model("incremental/mpp/jvmOnly", extension = null, excludeParentDirs = true)
            }
            testClass<AbstractIncrementalMultiplatformJsCompilerRunnerTest> {
                model("incremental/mpp/allPlatforms", extension = null, excludeParentDirs = true)
            }
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
            testClass<AbstractIncrementalFirJvmWithPluginCompilerRunnerTest> {
                model("pureKotlin", extension = null, recursive = false, targetBackend = TargetBackend.JVM_IR)
            }
        }
    }

    generateTestGroupSuiteWithJUnit5 {
        val excludedFirTestdataPattern = "^(.+)\\.fir\\.kts?\$"

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
            testClass<AbstractBytecodeListingTestForAllOpen> {
                model("bytecodeListing", excludedPattern = excludedFirTestdataPattern)
            }
            testClass<AbstractIrBytecodeListingTestForAllOpen> {
                model("bytecodeListing", excludedPattern = excludedFirTestdataPattern)
            }
            testClass<AbstractFirPsiBytecodeListingTestForAllOpen> {
                model("bytecodeListing", excludedPattern = excludedFirTestdataPattern)
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
            testClass<AbstractBytecodeListingTestForNoArg> {
                model("bytecodeListing", excludedPattern = excludedFirTestdataPattern)
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
        }

        testGroup("plugins/kapt3/kapt3-compiler/tests-gen", "plugins/kapt3/kapt3-compiler/testData") {
            val annotations = listOf(annotation(Tag::class.java, "IgnoreJDK11"))
            testClass<AbstractKotlinKaptContextTest>(annotations = annotations) {
                model("kotlinRunner")
            }

            testClass<AbstractIrKotlinKaptContextTest>(annotations = annotations) {
                model("kotlinRunner")
            }

            testClass<AbstractClassFileToSourceStubConverterTest> {
                model("converter")
            }

            testClass<AbstractIrClassFileToSourceStubConverterTest> {
                model("converter")
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
