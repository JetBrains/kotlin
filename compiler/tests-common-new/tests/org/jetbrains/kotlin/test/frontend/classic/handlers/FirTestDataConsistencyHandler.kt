/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.frontend.classic.handlers

import org.jetbrains.kotlin.test.WrappedException
import org.jetbrains.kotlin.test.directives.FirDiagnosticsDirectives
import org.jetbrains.kotlin.test.directives.model.DirectivesContainer
import org.jetbrains.kotlin.test.directives.model.RegisteredDirectives
import org.jetbrains.kotlin.test.model.AfterAnalysisChecker
import org.jetbrains.kotlin.test.model.TestFile
import org.jetbrains.kotlin.test.services.*
import org.jetbrains.kotlin.test.util.convertLineSeparators
import org.jetbrains.kotlin.test.utils.firTestDataFile
import java.io.File

class FirTestDataConsistencyHandler(testServices: TestServices) : AfterAnalysisChecker(testServices) {
    override val directiveContainers: List<DirectivesContainer>
        get() = listOf(FirDiagnosticsDirectives)

    override fun check(failedAssertions: List<WrappedException>) {
        val moduleStructure = testServices.moduleStructure
        val testData = moduleStructure.originalTestDataFiles.first()
        if (testData.extension == "kts") return
        if (FirDiagnosticsDirectives.FIR_IDENTICAL in moduleStructure.allDirectives) return
        val firTestData = testData.firTestDataFile
        testServices.assertions.assertTrue(firTestData.exists()) {
            "FIR test data does not exist; run the corresponding FIR test to generate it"
        }
        testServices.assertions.assertEquals(firTestData.preprocessSource(), testData.preprocessSource()) {
            "Original and fir test data aren't identical. " +
                    "Please, add changes from ${testData.name} to ${firTestData.name}"
        }
    }

    private fun File.preprocessSource(): String =
        testServices.sourceFileProvider.getContentOfSourceFile(
            TestFile(path, readText().trim().convertLineSeparators(), this, 0, isAdditional = false, RegisteredDirectives.Empty)
        )
}
