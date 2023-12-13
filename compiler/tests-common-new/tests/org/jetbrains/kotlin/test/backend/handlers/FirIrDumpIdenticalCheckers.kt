/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.backend.handlers

import org.jetbrains.kotlin.test.WrappedException
import org.jetbrains.kotlin.test.directives.CodegenTestDirectives.DUMP_IR
import org.jetbrains.kotlin.test.directives.FirDiagnosticsDirectives
import org.jetbrains.kotlin.test.directives.FirDiagnosticsDirectives.FIR_IDENTICAL
import org.jetbrains.kotlin.test.directives.model.DirectivesContainer
import org.jetbrains.kotlin.test.model.AfterAnalysisChecker
import org.jetbrains.kotlin.test.model.FrontendKinds
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.defaultsProvider
import org.jetbrains.kotlin.test.services.moduleStructure
import org.jetbrains.kotlin.test.utils.FirIdenticalCheckerHelper
import org.jetbrains.kotlin.test.utils.withExtension
import java.io.File

class FirIrDumpIdenticalChecker(testServices: TestServices) : AfterAnalysisChecker(testServices) {
    override val directiveContainers: List<DirectivesContainer>
        get() = listOf(FirDiagnosticsDirectives)

    private val simpleDumpChecker = object : FirIdenticalCheckerHelper(testServices) {
        override fun getClassicFileToCompare(testDataFile: File): File? {
            return testDataFile.withExtension(IrTextDumpHandler.DUMP_EXTENSION).takeIf { it.exists() }
        }

        override fun getFirFileToCompare(testDataFile: File): File? {
            return testDataFile.withExtension("fir.${IrTextDumpHandler.DUMP_EXTENSION}").takeIf { it.exists() }
        }
    }

    private val prettyDumpChecker = object : FirIdenticalCheckerHelper(testServices) {
        override fun getClassicFileToCompare(testDataFile: File): File? {
            return testDataFile.withExtension(IrPrettyKotlinDumpHandler.DUMP_EXTENSION).takeIf { it.exists() }
        }

        override fun getFirFileToCompare(testDataFile: File): File? {
            return testDataFile.withExtension("fir.${IrPrettyKotlinDumpHandler.DUMP_EXTENSION}").takeIf { it.exists() }
        }
    }

    override fun check(failedAssertions: List<WrappedException>) {
        if (failedAssertions.isNotEmpty()) return
        val testDataFile = testServices.moduleStructure.originalTestDataFiles.first()
        if (testServices.defaultsProvider.defaultFrontend != FrontendKinds.FIR)
            return
        if (DUMP_IR !in testServices.moduleStructure.allDirectives)
            return
        if (FIR_IDENTICAL in testServices.moduleStructure.allDirectives) {
            simpleDumpChecker.deleteFirFile(testDataFile)
            prettyDumpChecker.deleteFirFile(testDataFile)
            return
        }
        if (
            simpleDumpChecker.firAndClassicContentsAreEquals(testDataFile) &&
            prettyDumpChecker.firAndClassicContentsAreEquals(testDataFile, trimLines = true)
        ) {
            simpleDumpChecker.deleteFirFile(testDataFile)
            prettyDumpChecker.deleteFirFile(testDataFile)
            simpleDumpChecker.addDirectiveToClassicFileAndAssert(testDataFile)
        }
    }
}
