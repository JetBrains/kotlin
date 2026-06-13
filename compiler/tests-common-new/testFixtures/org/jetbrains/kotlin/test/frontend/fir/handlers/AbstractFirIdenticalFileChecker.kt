/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.frontend.fir.handlers

import org.jetbrains.kotlin.test.isTeamCityBuild
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.assertions
import org.jetbrains.kotlin.test.utils.*
import java.io.File

abstract class AbstractFirIdenticalFileChecker(testServices: TestServices) : AbstractAlternativeKtFileIdenticalChecker(testServices) {
    override fun checkTestDataFile(testDataFile: File) {
        if (!testDataFile.isRelevantTestData()) return
        val originalFile = testDataFile.originalTestDataFile
        if (contentsAreEquals(originalFile, testDataFile)) {
            testServices.assertions.assertAll(
                buildList {
                    add { deleteFirFileToCompareAndAssertIfExists(testDataFile) }

                    listOf(
                        originalFile.originalTestDataFile,
                        originalFile.firTestDataFile,
                        originalFile.llFirTestDataFile,
                        originalFile.reversedTestDataFile,
                        originalFile.partialBodyTestDataFile,
                        originalFile.latestLVTestDataFile,
                        originalFile.lfDisabledTestDataFile,
                    ).filter { it.exists() && !it.isRelevantTestData() }
                        .mapTo(this) { file ->
                            { updateClassicFileAndAssert(file) }
                        }
                }
            )
        }
    }

    private fun contentsAreEquals(classicFile: File, firFile: File): Boolean {
        val classicFileContent = readContent(classicFile, trimLines = false)
        val firFileContent = readContent(firFile, trimLines = false)
        return classicFileContent == firFileContent
    }

    protected abstract fun updateClassicFileAndAssert(testDataFile: File)

    private fun deleteFirFileToCompareAndAssertIfExists(testDataFile: File, suppressAssertion: Boolean = false) {
        val firFileToCompare = testDataFile.relevantTestDataFile().takeIf(File::exists) ?: return
        if (!isTeamCityBuild) {
            firFileToCompare.delete()
        }

        if (suppressAssertion) {
            return
        }

        val message = if (isTeamCityBuild) {
            "Please remove `${firFileToCompare.path}`"
        } else {
            "Deleted `${firFileToCompare.path}`"
        }

        testServices.assertions.fail {
            "$message\nPlease re-run the test"
        }
    }

    protected abstract val extension: String

    protected abstract fun File.relevantTestDataFile(): File

    protected abstract fun File.isRelevantTestData(): Boolean
}
