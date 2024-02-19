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

/**
 * Use this checker as a base class if you need to check if K1 and K2 dumps are the same
 *   and add/remove some directive to the testdata file if they actually are
 *
 * Format of dump filename:
 *  - K1: `testName.dumpExtension`
 *  - K2: `testName.fir.dumpExtension`
 *
 * By default, this checker uses existence of `FIR_IDENTICAL` directive as a marker that dumps are identical
 * This behavior can be tweaked by overriding [markedAsIdentical] and [processClassicFileIfContentIsIdentical] functions
 *
 * For example, [IrMangledNameAndSignatureDumpHandler.IdenticalChecker] uses lack of `SEPARATE_SIGNATURE_DUMP_FOR_K2` directive
 *   for this purpose
 */
abstract class SimpleFirIrIdenticalChecker(testServices: TestServices) : AfterAnalysisChecker(testServices) {
    protected abstract val dumpExtension: String

    override val directiveContainers: List<DirectivesContainer>
        get() = listOf(FirDiagnosticsDirectives)

    protected val simpleChecker = object : FirIdenticalCheckerHelper(testServices) {
        override fun getClassicFileToCompare(testDataFile: File): File? {
            return testDataFile.withExtension(dumpExtension).takeIf { it.exists() }
        }

        override fun getFirFileToCompare(testDataFile: File): File? {
            return testDataFile.withExtension("fir.${dumpExtension}").takeIf { it.exists() }
        }
    }

    protected open fun shouldRun(): Boolean {
        return true
    }

    override fun check(failedAssertions: List<WrappedException>) {
        if (!shouldRun()) return
        if (failedAssertions.isNotEmpty()) return
        val testDataFile = testServices.moduleStructure.originalTestDataFiles.first()
        if (markedAsIdentical()) {
            simpleChecker.deleteFirFile(testDataFile)
            return
        }
        if (simpleChecker.firAndClassicContentsAreEquals(testDataFile)) {
            simpleChecker.deleteFirFile(testDataFile)
            processClassicFileIfContentIsIdentical(testDataFile)
        }
    }

    protected open fun markedAsIdentical(): Boolean {
        return FirDiagnosticsDirectives.FIR_IDENTICAL in testServices.moduleStructure.allDirectives
    }

    protected open fun processClassicFileIfContentIsIdentical(testDataFile: File) {
        simpleChecker.addDirectiveToClassicFileAndAssert(testDataFile)
    }
}
