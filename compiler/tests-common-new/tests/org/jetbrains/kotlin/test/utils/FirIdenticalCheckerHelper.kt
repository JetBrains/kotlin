/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.utils

import org.jetbrains.kotlin.test.directives.FirDiagnosticsDirectives
import org.jetbrains.kotlin.test.directives.model.Directive
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.assertions
import org.jetbrains.kotlin.utils.addToStdlib.applyIf
import java.io.File

abstract class FirIdenticalCheckerHelper(private val testServices: TestServices) {
    companion object {
        val isTeamCityBuild: Boolean = System.getenv("TEAMCITY_VERSION") != null
    }

    abstract fun getClassicFileToCompare(testDataFile: File): File?
    abstract fun getFirFileToCompare(testDataFile: File): File?

    fun firAndClassicContentsAreEquals(testDataFile: File, trimLines: Boolean = false): Boolean {
        val classicFile = getClassicFileToCompare(testDataFile) ?: return false
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

    fun addDirectiveToClassicFileAndAssert(testDataFile: File, directiveToAdd: Directive = FirDiagnosticsDirectives.FIR_IDENTICAL) {
        if (!isTeamCityBuild) {
            val classicFileContent = testDataFile.readText()
            testDataFile.writer().use {
                it.appendLine("// ${directiveToAdd.name}")
                it.append(classicFileContent)
            }
        }

        val directiveName = directiveToAdd.name
        val message = if (isTeamCityBuild) {
            "Please remove .fir.txt dump and add // $directiveName to test source"
        } else {
            "Deleted .fir.txt dump, added // $directiveName to test source"
        }
        testServices.assertions.fail {
            """
                    Dumps via FIR & via old FE are the same. 
                    $message
                    Please re-run the test now
                """.trimIndent()
        }
    }

    fun removeDirectiveFromClassicFileAndAssert(testDataFile: File, directiveToRemove: Directive) {
        val directiveName = directiveToRemove.name
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
            "Please remove .fir.txt dump and remove // $directiveName from test source"
        } else {
            "Deleted .fir.txt dump, removed // $directiveName from test source"
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
