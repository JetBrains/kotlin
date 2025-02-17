/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.frontend.classic.handlers

import org.jetbrains.kotlin.test.WrappedException
import org.jetbrains.kotlin.test.directives.FirDiagnosticsDirectives
import org.jetbrains.kotlin.test.directives.FirDiagnosticsDirectives.FIR_IDENTICAL
import org.jetbrains.kotlin.test.directives.FirDiagnosticsDirectives.LATEST_LV_DIFFERENCE
import org.jetbrains.kotlin.test.directives.FirDiagnosticsDirectives.TEST_ALONGSIDE_K1_TESTDATA
import org.jetbrains.kotlin.test.directives.model.DirectivesContainer
import org.jetbrains.kotlin.test.directives.model.RegisteredDirectives
import org.jetbrains.kotlin.test.model.AfterAnalysisChecker
import org.jetbrains.kotlin.test.model.TestFile
import org.jetbrains.kotlin.test.runners.AbstractFirPsiDiagnosticsTestWithJvmIrBackend
import org.jetbrains.kotlin.test.runners.AbstractFirPsiDiagnosticTest
import org.jetbrains.kotlin.test.runners.AbstractKotlinCompilerTest
import org.jetbrains.kotlin.test.services.*
import org.jetbrains.kotlin.test.utils.firTestDataFile
import org.jetbrains.kotlin.test.utils.isFirTestData
import org.jetbrains.kotlin.test.utils.isLatestLVTestData
import org.jetbrains.kotlin.test.utils.originalTestDataFile
import java.io.File

open class FirTestDataConsistencyHandler(testServices: TestServices) : AfterAnalysisChecker(testServices) {
    override val directiveContainers: List<DirectivesContainer>
        get() = listOf(FirDiagnosticsDirectives)

    override fun check(failedAssertions: List<WrappedException>) {
        val moduleStructure = testServices.moduleStructure
        val testData = moduleStructure.originalTestDataFiles.first()
        if (testData.extension == "kts") return
        val directives = moduleStructure.allDirectives
        if (TEST_ALONGSIDE_K1_TESTDATA in directives && FIR_IDENTICAL !in directives) {
            checkK1AndFirTestData(testData)
        }
        if (LATEST_LV_DIFFERENCE in directives && testData.isLatestLVTestData) {
            checkFirAndLatestLVTestData(testData, directives)
        }
    }

    private fun checkK1AndFirTestData(testData: File) {
        val (firTestData, originalTestData) = when {
            testData.isFirTestData -> testData to testData.originalTestDataFile
            else -> testData.firTestDataFile to testData
        }
        if (!firTestData.exists()) {
            runFirTestAndGeneratedTestData(testData, firTestData)
            return
        }
        checkTwoFiles(originalTestData, firTestData, "Original and FIR test data aren't identical. ")
    }

    private fun checkFirAndLatestLVTestData(latestLVTestData: File, directives: RegisteredDirectives) {
        val firTestData = when {
            TEST_ALONGSIDE_K1_TESTDATA in directives && FIR_IDENTICAL !in directives -> latestLVTestData.firTestDataFile
            else -> latestLVTestData.originalTestDataFile
        }
        checkTwoFiles(firTestData, latestLVTestData, "Original and Latest Stable LV testdata aren't identical. ")
    }

    private fun checkTwoFiles(originalTestData: File, secondTestData: File, message: String) {
        val secondPreprocessedTextData = secondTestData.preprocessSource()
        val originalPreprocessedTextData = originalTestData.preprocessSource()
        testServices.assertions.assertEquals(secondPreprocessedTextData, originalPreprocessedTextData) {
            message + "Please, add changes from ${originalTestData.name} to ${secondTestData.name}"
        }
    }

    private fun File.preprocessSource(): String {
        val content = testServices.sourceFileProvider.getContentOfSourceFile(
            TestFile(path, readText().trim(), this, 0, isAdditional = false, RegisteredDirectives.Empty)
        )
        // Note: convertLineSeparators() does not work on Windows properly (\r\n are left intact for some reason)
        if (System.lineSeparator() != "\n") {
            return content.replace("\r\n", "\n")
        }
        return content
    }

    private fun runFirTestAndGeneratedTestData(testData: File, firTestData: File) {
        firTestData.writeText(testData.preprocessSource())
        val test = correspondingFirTest()
        test.initTestInfo(testServices.testInfo.copy(className = "${testServices.testInfo.className}_fir_anonymous"))
        test.runTest(firTestData.absolutePath)
    }

    protected open fun correspondingFirTest(): AbstractKotlinCompilerTest {
        return if ("Backend" in testServices.testInfo.className) object : AbstractFirPsiDiagnosticsTestWithJvmIrBackend() {}
        else object : AbstractFirPsiDiagnosticTest() {}
    }
}
