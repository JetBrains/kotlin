/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.frontend.fir.handlers

import org.jetbrains.kotlin.test.directives.FirDiagnosticsDirectives
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.assertions
import org.jetbrains.kotlin.test.utils.*
import java.io.File

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
        val originalFile = testDataFile.originalTestDataFile
        val helper = Helper(originalFile)
        if (helper.contentsAreEquals(originalFile, testDataFile)) {
            testServices.assertions.assertAll(
                buildList {
                    add {
                        helper.deleteFirFileToCompareAndAssertIfExists(testDataFile)
                    }

                    listOf(
                        originalFile.originalTestDataFile,
                        originalFile.firTestDataFile,
                        originalFile.llFirTestDataFile,
                        originalFile.reversedTestDataFile,
                        originalFile.partialBodyTestDataFile
                    ).filter { it.exists() }
                        .mapTo(this) { file ->
                            { helper.removeDirectiveFromClassicFileAndAssert(file, FirDiagnosticsDirectives.LATEST_LV_DIFFERENCE, message) }
                        }
                }
            )
        }
    }
}
