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

class LatestLVIdenticalChecker(testServices: TestServices) : AbstractFirIdenticalFileChecker(testServices) {

    override val extension: String
        get() = ".latestLV.kt"

    override fun File.isRelevantTestData(): Boolean = isLatestLVTestData

    override fun File.relevantTestDataFile(): File = latestLVTestDataFile

    override fun updateClassicFileAndAssert(testDataFile: File) {
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
            "Please remove $extension file and remove // $directiveName from test source"
        } else {
            "Deleted $extension file, removed // $directiveName from test source"
        }
        testServices.assertions.fail {
            """
                    Default and $extension dumps are the same. 
                    $message
                    Please re-run the test now
                """.trimIndent()
        }
    }

}

