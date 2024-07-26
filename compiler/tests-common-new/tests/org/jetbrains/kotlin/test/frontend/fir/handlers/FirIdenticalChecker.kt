/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.frontend.fir.handlers

import org.jetbrains.kotlin.test.directives.FirDiagnosticsDirectives
import org.jetbrains.kotlin.test.directives.FirDiagnosticsDirectives.FIR_IDENTICAL
import org.jetbrains.kotlin.test.directives.FirDiagnosticsDirectives.TEST_ALONGSIDE_K1_TESTDATA
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.assertions
import org.jetbrains.kotlin.test.services.moduleStructure
import org.jetbrains.kotlin.test.utils.*
import java.io.File

class FirIdenticalChecker(testServices: TestServices) : AbstractFirIdenticalChecker(testServices) {
    override fun checkTestDataFile(testDataFile: File) {
        // Skip `.ll.kt` test files, which are instead checked by `LLFirIdenticalChecker`.
        if (testDataFile.isLLFirTestData) return
        if (testDataFile.isLatestLVTestData) return

        if (testDataFile.isFirTestData) {
            val helper = Helper()
            val classicFile = helper.getClassicFileToCompare(testDataFile)
            if (".reversed." in classicFile.path) return

            if (helper.contentsAreEquals(classicFile, testDataFile, trimLines = true)) {
                helper.deleteFirFile(testDataFile)
                helper.addDirectiveToClassicFileAndAssert(classicFile)
            }
        } else {
            removeFirFileIfExist(testDataFile)
        }
    }

    private fun removeFirFileIfExist(testDataFile: File) {
        val firFile = Helper().getFirFileToCompare(testDataFile)
        firFile.delete()
    }
}

class LatestLVIdenticalChecker(testServices: TestServices) : AbstractFirIdenticalChecker(testServices) {
    companion object {
        private const val message: String = "Dumps with latest and latest stable LV are the same"
    }

    private inner class Helper(val originalFile: File) : FirIdenticalCheckerHelper(testServices) {
        override fun getClassicFileToCompare(testDataFile: File): File = originalFile
        override fun getFirFileToCompare(testDataFile: File): File = testDataFile.latestLVTestDataFile
    }

    override fun checkTestDataFile(testDataFile: File) {
        if (!testDataFile.isLatestLVTestData) return
        val directives = testServices.moduleStructure.allDirectives
        val (originalFile, additionalFile) = when {
            TEST_ALONGSIDE_K1_TESTDATA in directives && FIR_IDENTICAL !in directives -> testDataFile.firTestDataFile to testDataFile.originalTestDataFile
            else -> testDataFile.originalTestDataFile to null
        }
        val helper = Helper(originalFile)
        if (helper.contentsAreEquals(originalFile, testDataFile)) {
            helper.deleteFirFile(testDataFile)
            testServices.assertions.assertAll(
                { helper.removeDirectiveFromClassicFileAndAssert(originalFile, FirDiagnosticsDirectives.LATEST_LV_DIFFERENCE, message) },
                {
                    if (additionalFile != null) {
                        helper.removeDirectiveFromClassicFileAndAssert(additionalFile, FirDiagnosticsDirectives.LATEST_LV_DIFFERENCE, message)
                    }
                }
            )
        }
    }
}
