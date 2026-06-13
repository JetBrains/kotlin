/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.frontend.fir.handlers

import org.jetbrains.kotlin.test.directives.LanguageSettingsDirectives.LANGUAGE_FEATURE_TOGGLED_IDENTICAL
import org.jetbrains.kotlin.test.isTeamCityBuild
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.assertions
import org.jetbrains.kotlin.test.utils.isLfDisabledTestData
import org.jetbrains.kotlin.test.utils.lfDisabledTestDataFile
import java.io.File

class LfDisabledIdenticalChecker(testServices: TestServices) : AbstractFirIdenticalFileChecker(testServices) {

    override val extension: String
        get() = ".disabled.kt"

    override fun File.isRelevantTestData(): Boolean = isLfDisabledTestData

    override fun File.relevantTestDataFile(): File = lfDisabledTestDataFile

    override fun updateClassicFileAndAssert(testDataFile: File) {
        if (!isTeamCityBuild) {
            val classicFileContent = testDataFile.readLines()
            testDataFile.writer().use {
                it.appendLine("// $LANGUAGE_FEATURE_TOGGLED_IDENTICAL")
                classicFileContent.forEachIndexed { index, line ->
                    // Don't add empty line at the end, if last line is empty already
                    if (index == classicFileContent.size - 1 && line.isBlank()) return@forEachIndexed
                    it.appendLine(line)
                }
            }
        }
        val message = if (isTeamCityBuild) {
            "Please remove $extension file and add // $LANGUAGE_FEATURE_TOGGLED_IDENTICAL"
        } else {
            "Deleted $extension file, added // $LANGUAGE_FEATURE_TOGGLED_IDENTICAL to test source"
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
