/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.generators.tests

import org.jetbrains.kotlin.allopen.AbstractBytecodeListingTestForAllOpen
import org.jetbrains.kotlin.allopen.AbstractIrBytecodeListingTestForAllOpen
import org.jetbrains.kotlin.android.parcel.AbstractParcelBoxTest
import org.jetbrains.kotlin.android.parcel.AbstractParcelBytecodeListingTest
import org.jetbrains.kotlin.android.parcel.AbstractParcelIrBoxTest
import org.jetbrains.kotlin.android.parcel.AbstractParcelIrBytecodeListingTest
import org.jetbrains.kotlin.android.synthetic.test.AbstractAndroidBoxTest
import org.jetbrains.kotlin.android.synthetic.test.AbstractAndroidBytecodeShapeTest
import org.jetbrains.kotlin.android.synthetic.test.AbstractAndroidIrBoxTest
import org.jetbrains.kotlin.android.synthetic.test.AbstractAndroidSyntheticPropertyDescriptorTest
import org.jetbrains.kotlin.fir.plugin.runners.AbstractFirPluginBlackBoxCodegenTest
import org.jetbrains.kotlin.fir.plugin.runners.AbstractFirPluginDiagnosticTest
import org.jetbrains.kotlin.generators.TestGroup
import org.jetbrains.kotlin.generators.generateTestGroupSuiteWithJUnit5
import org.jetbrains.kotlin.generators.impl.generateTestGroupSuite
import org.jetbrains.kotlin.incremental.*
import org.jetbrains.kotlin.jvm.abi.*
import org.jetbrains.kotlin.kapt.cli.test.AbstractArgumentParsingTest
import org.jetbrains.kotlin.kapt.cli.test.AbstractKaptToolIntegrationTest
import org.jetbrains.kotlin.kapt3.test.AbstractClassFileToSourceStubConverterTest
import org.jetbrains.kotlin.kapt3.test.AbstractIrClassFileToSourceStubConverterTest
import org.jetbrains.kotlin.kapt3.test.AbstractIrKotlinKaptContextTest
import org.jetbrains.kotlin.kapt3.test.AbstractKotlinKaptContextTest
import org.jetbrains.kotlin.lombok.AbstractLombokCompileTest
import org.jetbrains.kotlin.noarg.*
import org.jetbrains.kotlin.parcelize.test.runners.*
import org.jetbrains.kotlin.samWithReceiver.AbstractSamWithReceiverScriptTest
import org.jetbrains.kotlin.samWithReceiver.AbstractSamWithReceiverTest
import org.jetbrains.kotlin.test.TargetBackend
import org.jetbrains.kotlinx.serialization.AbstractSerializationIrBytecodeListingTest
import org.jetbrains.kotlinx.serialization.AbstractSerializationPluginBytecodeListingTest
import org.jetbrains.kotlinx.serialization.AbstractSerializationPluginDiagnosticTest
import org.jetbrains.kotlinx.atomicfu.AbstractAtomicfuJsIrTest

fun main(args: Array<String>) {
    System.setProperty("java.awt.headless", "true")
    generateTestGroupSuite(args) {
        testGroup("compiler/incremental-compilation-impl/test", "jps/jps-plugin/testData") {
            fun incrementalJvmTestData(targetBackend: TargetBackend): TestGroup.TestClass.() -> Unit = {
                model("incremental/pureKotlin", extension = null, recursive = false, targetBackend = targetBackend)
                model("incremental/classHierarchyAffected", extension = null, recursive = false, targetBackend = targetBackend)
                model("incremental/inlineFunCallSite", extension = null, excludeParentDirs = true, targetBackend = targetBackend)
                model("incremental/withJava", extension = null, excludeParentDirs = true, targetBackend = targetBackend)
                model("incremental/incrementalJvmCompilerOnly", extension = null, excludeParentDirs = true, targetBackend = targetBackend)
            }
            testClass<AbstractIncrementalJvmCompilerRunnerTest>(init = incrementalJvmTestData(TargetBackend.JVM_IR))
            testClass<AbstractIncrementalJvmOldBackendCompilerRunnerTest>(init = incrementalJvmTestData(TargetBackend.JVM))
            testClass<AbstractIncrementalFirJvmCompilerRunnerTest>(init = incrementalJvmTestData(TargetBackend.JVM_IR))

            testClass<AbstractIncrementalJsCompilerRunnerTest> {
                model("incremental/pureKotlin", extension = null, recursive = false)
                model("incremental/classHierarchyAffected", extension = null, recursive = false)
                model("incremental/js", extension = null, excludeParentDirs = true)
            }

            testClass<AbstractIncrementalJsKlibCompilerRunnerTest>() {
                // IC of sealed interfaces are not supported in JS
                model("incremental/pureKotlin", extension = null, recursive = false, excludedPattern = "^sealed.*")
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
                model("incremental/pureKotlin", extension = null, recursive = false)
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
                model("compare", recursive = false, extension = null, targetBackend = TargetBackend.JVM)
            }

            testClass<AbstractJvmAbiContentTest> {
                model("content", recursive = false, extension = null, targetBackend = TargetBackend.JVM)
            }

            testClass<AbstractCompileAgainstJvmAbiTest> {
                model("compile", recursive = false, extension = null, targetBackend = TargetBackend.JVM)
            }

            testClass<AbstractIrCompareJvmAbiTest> {
                model("compare", recursive = false, extension = null, targetBackend = TargetBackend.JVM_IR)
            }

            testClass<AbstractIrJvmAbiContentTest> {
                model("content", recursive = false, extension = null, targetBackend = TargetBackend.JVM_IR)
            }

            testClass<AbstractIrCompileAgainstJvmAbiTest> {
                model("compile", recursive = false, extension = null, targetBackend = TargetBackend.JVM_IR)
            }
        }

        testGroup(
            "plugins/jvm-abi-gen/test", "plugins/jvm-abi-gen/testData",
            testRunnerMethodName = "runTestWithCustomIgnoreDirective",
            additionalRunnerArguments = listOf("\"// IGNORE_BACKEND_LEGACY: \"")
        ) {
            testClass<AbstractLegacyCompareJvmAbiTest> {
                model("compare", recursive = false, extension = null, targetBackend = TargetBackend.JVM)
            }

            testClass<AbstractLegacyJvmAbiContentTest> {
                model("content", recursive = false, extension = null, targetBackend = TargetBackend.JVM)
            }

            testClass<AbstractLegacyCompileAgainstJvmAbiTest> {
                model("compile", recursive = false, extension = null, targetBackend = TargetBackend.JVM)
            }
        }

        testGroup("plugins/kapt3/kapt3-compiler/test", "plugins/kapt3/kapt3-compiler/testData") {
            testClass<AbstractClassFileToSourceStubConverterTest> {
                model("converter")
            }

            testClass<AbstractKotlinKaptContextTest> {
                model("kotlinRunner")
            }

            testClass<AbstractIrClassFileToSourceStubConverterTest> {
                model("converter", targetBackend = TargetBackend.JVM_IR)
            }

            testClass<AbstractIrKotlinKaptContextTest> {
                model("kotlinRunner", targetBackend = TargetBackend.JVM_IR)
            }
        }

        testGroup("plugins/kapt3/kapt3-cli/test", "plugins/kapt3/kapt3-cli/testData") {
            testClass<AbstractArgumentParsingTest> {
                model("argumentParsing", extension = "txt")
            }

            testClass<AbstractKaptToolIntegrationTest> {
                model("integration", recursive = false, extension = null)
            }
        }

        testGroup("plugins/allopen/allopen-cli/test", "plugins/allopen/allopen-cli/testData") {
            testClass<AbstractBytecodeListingTestForAllOpen> {
                model("bytecodeListing", extension = "kt")
            }
            testClass<AbstractIrBytecodeListingTestForAllOpen> {
                model("bytecodeListing", extension = "kt")
            }
        }

        testGroup("plugins/noarg/noarg-cli/test", "plugins/noarg/noarg-cli/testData") {
            testClass<AbstractDiagnosticsTestForNoArg> { model("diagnostics", extension = "kt") }

            testClass<AbstractBytecodeListingTestForNoArg> {
                model("bytecodeListing", extension = "kt", targetBackend = TargetBackend.JVM)
            }
            testClass<AbstractIrBytecodeListingTestForNoArg> {
                model("bytecodeListing", extension = "kt", targetBackend = TargetBackend.JVM_IR)
            }

            testClass<AbstractBlackBoxCodegenTestForNoArg> { model("box", targetBackend = TargetBackend.JVM) }
            testClass<AbstractIrBlackBoxCodegenTestForNoArg> { model("box", targetBackend = TargetBackend.JVM_IR) }
        }

        testGroup("plugins/sam-with-receiver/sam-with-receiver-cli/test", "plugins/sam-with-receiver/sam-with-receiver-cli/testData") {
            testClass<AbstractSamWithReceiverTest> {
                model("diagnostics")
            }
            testClass<AbstractSamWithReceiverScriptTest> {
                model("script", extension = "kts")
            }
        }

        testGroup(
            "plugins/kotlin-serialization/kotlin-serialization-compiler/test",
            "plugins/kotlin-serialization/kotlin-serialization-compiler/testData"
        ) {
            testClass<AbstractSerializationPluginDiagnosticTest> {
                model("diagnostics")
            }

            testClass<AbstractSerializationPluginBytecodeListingTest> {
                model("codegen")
            }

            testClass<AbstractSerializationIrBytecodeListingTest> {
                model("codegen")
            }
        }

        testGroup("plugins/lombok/lombok-compiler-plugin/tests", "plugins/lombok/lombok-compiler-plugin/testData") {
            testClass<AbstractLombokCompileTest> {
                model("compile")
            }
        }
/*
    testGroup("plugins/android-extensions/android-extensions-idea/tests", "plugins/android-extensions/android-extensions-idea/testData") {
        testClass<AbstractAndroidCompletionTest> {
            model("android/completion", recursive = false, extension = null)
        }

        testClass<AbstractAndroidGotoTest> {
            model("android/goto", recursive = false, extension = null)
        }

        testClass<AbstractAndroidRenameTest> {
            model("android/rename", recursive = false, extension = null)
        }

        testClass<AbstractAndroidLayoutRenameTest> {
            model("android/renameLayout", recursive = false, extension = null)
        }

        testClass<AbstractAndroidFindUsagesTest> {
            model("android/findUsages", recursive = false, extension = null)
        }

        testClass<AbstractAndroidUsageHighlightingTest> {
            model("android/usageHighlighting", recursive = false, extension = null)
        }

        testClass<AbstractAndroidExtractionTest> {
            model("android/extraction", recursive = false, extension = null)
        }

        testClass<AbstractParcelCheckerTest> {
            model("android/parcel/checker", excludeParentDirs = true)
        }

        testClass<AbstractParcelQuickFixTest> {
            model("android/parcel/quickfix", pattern = """^(\w+)\.((before\.Main\.\w+)|(test))$""", testMethod = "doTestWithExtraFile")
        }
    }

    testGroup("idea/idea-android/tests", "idea/testData") {
        testClass<AbstractConfigureProjectTest> {
            model("configuration/android-gradle", pattern = """(\w+)_before\.gradle$""", testMethod = "doTestAndroidGradle")
            model("configuration/android-gsk", pattern = """(\w+)_before\.gradle.kts$""", testMethod = "doTestAndroidGradle")
        }

        testClass<AbstractAndroidIntentionTest> {
            model("android/intention", pattern = "^([\\w\\-_]+)\\.kt$")
        }

        testClass<AbstractAndroidResourceIntentionTest> {
            model("android/resourceIntention", extension = "test", singleClass = true)
        }

        testClass<AbstractAndroidQuickFixMultiFileTest> {
            model("android/quickfix", pattern = """^(\w+)\.((before\.Main\.\w+)|(test))$""", testMethod = "doTestWithExtraFile")
        }

        testClass<AbstractKotlinLintTest> {
            model("android/lint", excludeParentDirs = true)
        }

        testClass<AbstractAndroidLintQuickfixTest> {
            model("android/lintQuickfix", pattern = "^([\\w\\-_]+)\\.kt$")
        }

        testClass<AbstractAndroidResourceFoldingTest> {
            model("android/folding")
        }

        testClass<AbstractAndroidGutterIconTest> {
            model("android/gutterIcon")
        }
    }
*/

        testGroup("plugins/fir-plugin-prototype/fir-plugin-ic-test/tests-gen", "plugins/fir-plugin-prototype/fir-plugin-ic-test/testData") {
            testClass<AbstractIncrementalFirJvmWithPluginCompilerRunnerTest> {
                model("pureKotlin", extension = null, recursive = false, targetBackend = TargetBackend.JVM_IR)
            }
        }
    }

    generateTestGroupSuiteWithJUnit5 {
        val excludedFirTestdataPattern = "^(.+)\\.fir\\.kts?\$"

        testGroup("plugins/parcelize/parcelize-compiler/tests-gen", "plugins/parcelize/parcelize-compiler/testData") {
            testClass<AbstractParcelizeBoxTest> {
                model("box")
            }

            testClass<AbstractParcelizeIrBoxTest> {
                model("box")
            }

            testClass<AbstractParcelizeBytecodeListingTest> {
                model("codegen")
            }

            testClass<AbstractParcelizeIrBytecodeListingTest> {
                model("codegen")
            }

            testClass<AbstractParcelizeDiagnosticTest> {
                model("diagnostics", excludedPattern = excludedFirTestdataPattern)
            }
        }

        testGroup("plugins/fir-plugin-prototype/tests-gen", "plugins/fir-plugin-prototype/testData") {
            testClass<AbstractFirPluginDiagnosticTest> {
                model("diagnostics")
            }

            testClass<AbstractFirPluginBlackBoxCodegenTest> {
                model("box")
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
    }
}
