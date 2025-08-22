/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.generators.tests

import org.jetbrains.kotlin.assignment.plugin.AbstractAssignmentPluginDiagnosticTest
import org.jetbrains.kotlin.assignment.plugin.AbstractFirLightTreeBlackBoxCodegenTestForAssignmentPlugin
import org.jetbrains.kotlin.assignment.plugin.AbstractFirPsiAssignmentPluginDiagnosticTest
import org.jetbrains.kotlin.assignment.plugin.AbstractIrBlackBoxCodegenTestAssignmentPlugin
import org.jetbrains.kotlin.compiler.plugins.AbstractPluginInteractionFirBlackBoxCodegenTest
import org.jetbrains.kotlin.fir.dataframe.AbstractCompilerFacilityTestForDataFrame
import org.jetbrains.kotlin.fir.dataframe.AbstractDataFrameBlackBoxCodegenTest
import org.jetbrains.kotlin.fir.dataframe.AbstractDataFrameDiagnosticTest
import org.jetbrains.kotlin.generators.generateTestGroupSuiteWithJUnit5
import org.jetbrains.kotlin.generators.util.TestGeneratorUtil
import org.jetbrains.kotlin.kapt.cli.test.AbstractArgumentParsingTest
import org.jetbrains.kotlin.kapt.cli.test.AbstractFirKaptToolIntegrationTest
import org.jetbrains.kotlin.kapt.cli.test.AbstractKaptToolIntegrationTest
import org.jetbrains.kotlin.kapt.test.AbstractFirKaptStubConverterTest
import org.jetbrains.kotlin.kapt.test.runners.AbstractIrKotlinKaptContextTest
import org.jetbrains.kotlin.kapt.test.runners.AbstractKaptStubConverterTest
import org.jetbrains.kotlin.powerassert.AbstractFirLightTreeBlackBoxCodegenTestForPowerAssert
import org.jetbrains.kotlin.powerassert.AbstractIrBlackBoxCodegenTestForPowerAssert
import org.jetbrains.kotlin.scripting.test.*


fun main(args: Array<String>) {
    System.setProperty("java.awt.headless", "true")

    generateTestGroupSuiteWithJUnit5 {
        testGroup("plugins/power-assert/tests-gen", "plugins/power-assert/testData") {
            testClass<AbstractIrBlackBoxCodegenTestForPowerAssert> {
                model("codegen", excludedPattern = TestGeneratorUtil.KT_OR_KTS_WITH_FIR_PREFIX)
            }
            testClass<AbstractFirLightTreeBlackBoxCodegenTestForPowerAssert> {
                model("codegen", excludedPattern = TestGeneratorUtil.KT_OR_KTS_WITH_FIR_PREFIX)
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
                model("diagnostics", excludedPattern = TestGeneratorUtil.KT_OR_KTS_WITH_FIR_PREFIX)
            }
            testClass<AbstractFirPsiAssignmentPluginDiagnosticTest> {
                model("diagnostics", excludedPattern = TestGeneratorUtil.KT_OR_KTS_WITH_FIR_PREFIX)
            }
            testClass<AbstractIrBlackBoxCodegenTestAssignmentPlugin> {
                model("codegen", excludedPattern = TestGeneratorUtil.KT_OR_KTS_WITH_FIR_PREFIX)
            }
            testClass<AbstractFirLightTreeBlackBoxCodegenTestForAssignmentPlugin> {
                model("codegen", excludedPattern = TestGeneratorUtil.KT_OR_KTS_WITH_FIR_PREFIX)
            }
        }

        testGroup("plugins/plugins-interactions-testing/tests-gen", "plugins/plugins-interactions-testing/testData") {
            testClass<AbstractPluginInteractionFirBlackBoxCodegenTest> {
                model("box", excludedPattern = TestGeneratorUtil.KT_OR_KTS_WITH_FIR_PREFIX)
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
