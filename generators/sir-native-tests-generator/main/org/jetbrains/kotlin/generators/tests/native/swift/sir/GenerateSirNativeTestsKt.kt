/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.generators.tests.native.swift.sir

import org.jetbrains.kotlin.generators.generateTestGroupSuiteWithJUnit5
import org.jetbrains.kotlin.swiftexport.AbstractSwiftExportContextTest

fun main() {
    System.setProperty("java.awt.headless", "true")
    generateTestGroupSuiteWithJUnit5 {
        testGroup(
            "plugins/swift-export/tests-gen",
            "plugins/swift-export/testData"
        ) {
            testClass<AbstractSwiftExportContextTest>(
                suiteTestClassName = "SwiftExportCompilerPluginTest"
            ) {
                model("", excludedPattern = ".*\\.golden\\.kt$", recursive = false)
            }
        }
    }
}
