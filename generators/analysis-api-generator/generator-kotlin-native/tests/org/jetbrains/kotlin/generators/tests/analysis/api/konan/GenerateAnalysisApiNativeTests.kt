/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.generators.tests.analysis.api.konan

import org.jetbrains.kotlin.generators.tests.analysis.api.dsl.AnalysisApiTestGroup
import org.jetbrains.kotlin.generators.tests.analysis.api.dsl.generate

fun main(args: Array<String>) {
    System.setProperty("java.awt.headless", "true")
    generate(args) {
        generateTests()
    }
}

private fun AnalysisApiTestGroup.generateTests() {
    suiteBasedTests {
        generateFirNativeLowLevelTests()
    }
}
