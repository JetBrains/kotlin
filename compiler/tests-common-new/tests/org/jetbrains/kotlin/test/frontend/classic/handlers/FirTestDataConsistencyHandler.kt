/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.frontend.classic.handlers

import org.jetbrains.kotlin.codeMetaInfo.clearTextFromDiagnosticMarkup
import org.jetbrains.kotlin.test.directives.FirDiagnosticsDirectives
import org.jetbrains.kotlin.test.directives.model.DirectivesContainer
import org.jetbrains.kotlin.test.model.AfterAnalysisChecker
import org.jetbrains.kotlin.test.runners.AbstractFirDiagnosticTest
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.assertions
import org.jetbrains.kotlin.test.services.moduleStructure
import org.jetbrains.kotlin.test.services.testInfo
import org.jetbrains.kotlin.test.utils.firTestDataFile
import java.io.File

class FirTestDataConsistencyHandler(testServices: TestServices) : AfterAnalysisChecker(testServices) {
    override val directives: List<DirectivesContainer>
        get() = listOf(FirDiagnosticsDirectives)

    override fun check(failedAssertions: List<Throwable>) {
        val moduleStructure = testServices.moduleStructure
        val testData = moduleStructure.originalTestDataFiles.first()
        if (testData.extension == "kts") return
        if (FirDiagnosticsDirectives.FIR_IDENTICAL in moduleStructure.allDirectives) return
        val firTestData = testData.firTestDataFile
        if (!firTestData.exists()) {
            runFirTestAndGeneratedTestData(testData, firTestData)
            return
        }
        val originalFileContent = clearTextFromDiagnosticMarkup(testData.readText())
        val firFileContent = clearTextFromDiagnosticMarkup(firTestData.readText())
        testServices.assertions.assertEquals(originalFileContent, firFileContent) {
            "Original and fir test data aren't identical. " +
                    "Please, add changes from ${testData.name} to ${firTestData.name}"
        }
    }

    private fun runFirTestAndGeneratedTestData(testData: File, firTestData: File) {
        firTestData.writeText(clearTextFromDiagnosticMarkup(testData.readText()))
        val test = object : AbstractFirDiagnosticTest() {}
        test.initTestInfo(testServices.testInfo.copy(className = "${testServices.testInfo.className}_fir_anonymous"))
        test.runTest(firTestData.absolutePath)
    }
}
