/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.frontend.fir.handlers

import org.jetbrains.kotlin.test.directives.FirDiagnosticsDirectives
import org.jetbrains.kotlin.test.model.AfterAnalysisChecker
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.assertions
import org.jetbrains.kotlin.test.services.moduleStructure
import org.jetbrains.kotlin.test.utils.firTestDataFile
import org.jetbrains.kotlin.test.utils.isFirTestData
import org.jetbrains.kotlin.test.utils.originalTestDataFile
import java.io.File

class FirIdenticalChecker(testServices: TestServices) : AfterAnalysisChecker(testServices) {
    override fun check(failedAssertions: List<AssertionError>) {
        if (failedAssertions.isNotEmpty()) return
        val file = testServices.moduleStructure.originalTestDataFiles.first()
        if (file.isFirTestData) {
            addDirectiveToClassicFile(file)
        } else {
            removeFirFileIfExist(file)
        }
    }

    private fun addDirectiveToClassicFile(firFile: File) {
        val classicFile = firFile.originalTestDataFile
        val classicFileContent = classicFile.readText()
        val firFileContent = firFile.readText()
        if (classicFileContent == firFileContent) {
            classicFile.writer().use {
                it.appendLine("// ${FirDiagnosticsDirectives.FIR_IDENTICAL.name}")
                it.append(classicFileContent)
            }
            firFile.delete()
            testServices.assertions.fail {
                """
                    Dumps via FIR & via old FE are the same. 
                    Deleted .fir.txt dump, added // FIR_IDENTICAL to test source
                    Please re-run the test now
                """.trimIndent()
            }
        }
    }

    private fun removeFirFileIfExist(classicFile: File) {
        val firFile = classicFile.firTestDataFile
        firFile.delete()
    }
}
