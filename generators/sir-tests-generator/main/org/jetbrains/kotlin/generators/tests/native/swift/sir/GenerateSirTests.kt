/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.generators.tests.native.swift.sir

import org.jetbrains.kotlin.analysis.api.fir.test.configurators.AnalysisApiFirTestConfiguratorFactory
import org.jetbrains.kotlin.analysis.test.framework.test.configurators.*
import org.jetbrains.kotlin.swiftexport.ide.AbstractSymbolToSirTest
import org.jetbrains.kotlin.generators.generateTestGroupSuiteWithJUnit5
import org.jetbrains.kotlin.generators.tests.analysis.api.dsl.FrontendConfiguratorTestGenerator
import org.jetbrains.kotlin.generators.tests.analysis.api.dsl.FrontendConfiguratorTestModel
import org.jetbrains.kotlin.generators.tests.provider
import org.jetbrains.kotlin.konan.test.blackbox.support.group.UseStandardTestCaseGroupProvider
import org.jetbrains.kotlin.sir.bridge.AbstractKotlinSirBridgeTest
import org.jetbrains.kotlin.swiftexport.standalone.test.AbstractKlibBasedSwiftRunnerTest
import org.jetbrains.kotlin.swiftexport.standalone.test.AbstractSwiftExportExecutionTest


fun main() {
    System.setProperty("java.awt.headless", "true")
    generateTestGroupSuiteWithJUnit5(additionalMethodGenerators = listOf(FrontendConfiguratorTestGenerator)) {
        testGroup(
            "native/swift/sir-compiler-bridge/tests-gen/",
            "native/swift/sir-compiler-bridge/testData"
        ) {
            testClass<AbstractKotlinSirBridgeTest>(
                suiteTestClassName = "SirCompilerBridgeTestGenerated"
            ) {
                model("", extension = null, recursive = false)
            }
        }
        testGroup(
            "native/swift/swift-export-standalone-integration-tests/simple/tests-gen/",
            "native/swift/swift-export-standalone-integration-tests/simple/testData/generation"
        ) {
            testClass<AbstractKlibBasedSwiftRunnerTest>(
                suiteTestClassName = "KlibBasedSwiftExportRunnerTest",
                annotations = listOf(
                    provider<UseStandardTestCaseGroupProvider>(),
                ),
            ) {
                model("", extension = null, recursive = false)
            }
        }
        // Swift Export Standalone
        testGroup(
            "native/swift/swift-export-standalone-integration-tests/simple/tests-gen/",
            "native/swift/swift-export-standalone-integration-tests/simple/testData/execution"
        ) {
            testClass<AbstractSwiftExportExecutionTest>(
                suiteTestClassName = "SwiftExportExecutionTestGenerated",
                annotations = listOf(
                    provider<UseStandardTestCaseGroupProvider>(),
                ),
            ) {
                model(pattern = "^([^_](.+))$", recursive = false)
            }
        }
        // Swift Export IDE
        testGroup(
            "native/swift/swift-export-ide/tests-gen/",
            "native/swift/swift-export-ide/testData"
        ) {
            testClass<AbstractSymbolToSirTest>(
                suiteTestClassName = "SwiftExportInIdeTestGenerated",
            ) {
                model(recursive = false)
                val data = AnalysisApiTestConfiguratorFactoryData(
                    FrontendKind.Fir,
                    TestModuleKind.Source,
                    AnalysisSessionMode.Normal,
                    AnalysisApiMode.Ide
                )
                method(FrontendConfiguratorTestModel(AnalysisApiFirTestConfiguratorFactory::class, data))
            }
        }
    }
}
