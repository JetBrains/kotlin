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
import org.jetbrains.kotlin.generators.util.TestGeneratorUtil
import org.jetbrains.kotlin.incremental.AbstractIncrementalK2JvmWithPluginCompilerRunnerTest
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
import org.jetbrains.kotlin.powerassert.AbstractFirLightTreeBlackBoxCodegenTestForPowerAssert
import org.jetbrains.kotlin.powerassert.AbstractIrBlackBoxCodegenTestForPowerAssert
import org.jetbrains.kotlin.samWithReceiver.*
import org.jetbrains.kotlin.scripting.test.*
import org.jetbrains.kotlin.test.TargetBackend


fun main(args: Array<String>) {
    System.setProperty("java.awt.headless", "true")
    generateTestGroupSuite(args) {

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
                model("integration-k1", recursive = false, extension = null)
            }
            testClass<AbstractFirKaptToolIntegrationTest> {
                model("integration", recursive = false, extension = null)
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
