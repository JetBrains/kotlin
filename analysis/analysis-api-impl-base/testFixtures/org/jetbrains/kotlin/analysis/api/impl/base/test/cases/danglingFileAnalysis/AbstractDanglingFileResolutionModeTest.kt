/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.test.cases.danglingFileAnalysis

import org.jetbrains.kotlin.analysis.api.projectStructure.KaDanglingFileResolutionModeProvider
import org.jetbrains.kotlin.analysis.test.framework.base.AbstractAnalysisApiBasedTest
import org.jetbrains.kotlin.analysis.test.framework.projectStructure.ktTestModuleStructure
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.assertions

abstract class AbstractDanglingFileResolutionModeTest : AbstractAnalysisApiBasedTest() {
    override fun doTest(testServices: TestServices) {
        val testModules = testServices.ktTestModuleStructure.mainModules
        val originalFile = testModules.singleOrNull { it.name == "original" }?.ktFiles?.single()
            ?: error("No 'original' module was found. Expected `original` module with a single file.")
        val copyFile = testModules.singleOrNull { it.name == "copy" }?.ktFiles?.single()
            ?: error("No 'copy' module was found. Expected `copy` module with a single file.")
        copyFile.originalFile = originalFile
        val resolutionMode = KaDanglingFileResolutionModeProvider.calculateMode(copyFile)
        val result = buildString {
            appendLine("RESOLUTION_MODE: $resolutionMode")
        }
        testServices.assertions.assertEqualsToTestOutputFile(result)
    }
}