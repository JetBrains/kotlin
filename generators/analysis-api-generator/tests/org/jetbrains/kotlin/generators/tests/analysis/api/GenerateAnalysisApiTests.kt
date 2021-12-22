/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.generators.tests.analysis.api

import org.jetbrains.kotlin.generators.TestGroupSuite
import org.jetbrains.kotlin.generators.generateTestGroupSuiteWithJUnit5

fun main(args: Array<String>) {
    System.setProperty("java.awt.headless", "true")
    generateTestGroupSuiteWithJUnit5(args) {
        generateTests()
    }
}

private fun TestGroupSuite.generateTests() {
    generateAnalysisApiTests()
    generateFirLowLevelApiTests()
    generateDecompiledTests()
    generateSymbolLightClassesTests()
}
