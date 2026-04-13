/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.frontend.fir.handlers

import org.jetbrains.kotlin.test.directives.FirDiagnosticsDirectives.LATEST_LV_DIFFERENCE
import org.jetbrains.kotlin.test.isTeamCityBuild
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.assertions
import org.jetbrains.kotlin.test.utils.*
import java.io.File

class LatestLVIdenticalChecker(testServices: TestServices) : AbstractAlternativeKtFileIdenticalChecker(testServices) {
    override fun checkTestDataFile(testDataFile: File) {
        if (!testDataFile.isLatestLVTestData) return
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
                        originalFile.partialBodyTestDataFile
                    ).filter { it.exists() }
                        .mapTo(this) { file ->
                            { removeDirectiveFromClassicFileAndAssert(file) }
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

    private fun removeDirectiveFromClassicFileAndAssert(testDataFile: File) {
        val directiveName = LATEST_LV_DIFFERENCE.name
        if (!isTeamCityBuild) {
            val classicFileContent = testDataFile.readLines()
            testDataFile.writer().use {
                classicFileContent.forEachIndexed { index, line ->
                    if (line.startsWith("// $directiveName")) return@forEachIndexed
                    // Don't add empty line at the end, if last line is empty already
                    if (index == classicFileContent.size - 1 && line.isBlank()) return@forEachIndexed
                    it.appendLine(line)
                }
            }
        }
        val message = if (isTeamCityBuild) {
            "Please remove .latestLV.kt file and remove // $directiveName from test source"
        } else {
            "Deleted .latestLV.kt file, removed // $directiveName from test source"
        }
        testServices.assertions.fail {
            """
                    Dumps with latest and latest stable LV are the same. 
                    $message
                    Please re-run the test now
                """.trimIndent()
        }
    }

    private fun deleteFirFileToCompareAndAssertIfExists(testDataFile: File, suppressAssertion: Boolean = false) {
        val firFileToCompare = testDataFile.latestLVTestDataFile.takeIf(File::exists) ?: return
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
}
