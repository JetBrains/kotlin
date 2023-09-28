/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.backend.handlers

import org.jetbrains.kotlin.test.WrappedException
import org.jetbrains.kotlin.test.directives.FirDiagnosticsDirectives
import org.jetbrains.kotlin.test.directives.model.DirectivesContainer
import org.jetbrains.kotlin.test.model.AfterAnalysisChecker
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.moduleStructure
import org.jetbrains.kotlin.test.utils.FirIdenticalCheckerHelper
import org.jetbrains.kotlin.test.utils.withExtension
import java.io.File

class FirIrSourceRangesIdenticalChecker(testServices: TestServices) : AfterAnalysisChecker(testServices) {
    override val directiveContainers: List<DirectivesContainer>
        get() = listOf(FirDiagnosticsDirectives)

    private val simpleChecker = object : FirIdenticalCheckerHelper(testServices) {
        override fun getClassicFileToCompare(testDataFile: File): File? {
            return testDataFile.withExtension(IrSourceRangesDumpHandler.DUMP_EXTENSION).takeIf { it.exists() }
        }

        override fun getFirFileToCompare(testDataFile: File): File? {
            return testDataFile.withExtension("fir.${IrSourceRangesDumpHandler.DUMP_EXTENSION}").takeIf { it.exists() }
        }
    }

    override fun check(failedAssertions: List<WrappedException>) {
        if (failedAssertions.isNotEmpty()) return
        val testDataFile = testServices.moduleStructure.originalTestDataFiles.first()
        if (FirDiagnosticsDirectives.FIR_IDENTICAL in testServices.moduleStructure.allDirectives) {
            simpleChecker.deleteFirFile(testDataFile)
            return
        }
        if (simpleChecker.firAndClassicContentsAreEquals(testDataFile)) {
            simpleChecker.deleteFirFile(testDataFile)
            simpleChecker.addDirectiveToClassicFileAndAssert(testDataFile)
        }
    }
}
