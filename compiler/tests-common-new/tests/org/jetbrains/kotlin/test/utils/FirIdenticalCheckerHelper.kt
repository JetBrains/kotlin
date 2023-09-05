/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.utils

import org.jetbrains.kotlin.test.directives.FirDiagnosticsDirectives
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.assertions
import org.jetbrains.kotlin.utils.addToStdlib.applyIf
import java.io.File

abstract class FirIdenticalCheckerHelper(private val testServices: TestServices) {
    companion object {
        private val isTeamCityBuild: Boolean = System.getenv("TEAMCITY_VERSION") != null
    }

    abstract fun getClassicFileToCompare(testDataFile: File): File?
    abstract fun getFirFileToCompare(testDataFile: File): File?

    fun firAndClassicContentsAreEquals(testDataFile: File, trimLines: Boolean = false): Boolean {
        val classicFile = getClassicFileToCompare(testDataFile) ?: return true
        val firFile = getFirFileToCompare(testDataFile) ?: return true
        return contentsAreEquals(classicFile, firFile, trimLines)
    }

    fun contentsAreEquals(classicFile: File, firFile: File, trimLines: Boolean = false): Boolean {
        val classicFileContent = readContent(classicFile, trimLines)
        val firFileContent = readContent(firFile, trimLines)
        return classicFileContent == firFileContent
    }

    fun readContent(file: File, trimLines: Boolean): String {
        return if (trimLines) {
            file.readLines().joinToString("\n") { it.trimEnd() }.trim()
        } else {
            file.readText()
        }
    }

    fun addDirectiveToClassicFileAndAssert(testDataFile: File) {
        if (!isTeamCityBuild) {
            val classicFileContent = testDataFile.readText()
            testDataFile.writer().use {
                it.appendLine("// ${FirDiagnosticsDirectives.FIR_IDENTICAL.name}")
                it.append(classicFileContent)
            }
        }

        val message = if (isTeamCityBuild) {
            "Please remove .fir.txt dump and add // FIR_IDENTICAL to test source"
        } else {
            "Deleted .fir.txt dump, added // FIR_IDENTICAL to test source"
        }
        testServices.assertions.fail {
            """
                    Dumps via FIR & via old FE are the same. 
                    $message
                    Please re-run the test now
                """.trimIndent()
        }
    }

    fun deleteFirFile(testDataFile: File) {
        if (!isTeamCityBuild) {
            getFirFileToCompare(testDataFile)?.takeIf { it.exists() }?.delete()
        }
    }
}
