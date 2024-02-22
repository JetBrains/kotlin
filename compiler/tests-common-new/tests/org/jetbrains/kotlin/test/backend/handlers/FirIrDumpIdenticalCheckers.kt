/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.backend.handlers

import org.jetbrains.kotlin.test.WrappedException
import org.jetbrains.kotlin.test.directives.CodegenTestDirectives.DUMP_EXTERNAL_CLASS
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
import org.jetbrains.kotlin.test.utils.withSuffixAndExtension
import java.io.File

class FirIrDumpIdenticalChecker(testServices: TestServices) : AfterAnalysisChecker(testServices) {
    override val directiveContainers: List<DirectivesContainer>
        get() = listOf(FirDiagnosticsDirectives)

    private inner class DumpChecker(
        val suffix: String?,
        val classicExtension: String,
        val firExtension: String,
    ) : FirIdenticalCheckerHelper(testServices) {
        private fun withExtension(testDataFile: File, extension: String) : File {
            return if (suffix == null)
                testDataFile.withExtension(extension)
            else
                testDataFile.withSuffixAndExtension(suffix, extension)
        }

        override fun getClassicFileToCompare(testDataFile: File): File? {
            return withExtension(testDataFile, classicExtension).takeIf { it.exists() }
        }

        override fun getFirFileToCompare(testDataFile: File): File? {
            return withExtension(testDataFile, firExtension).takeIf { it.exists() }
        }
    }

    override fun check(failedAssertions: List<WrappedException>) {
        val dumpCheckers = buildList {
            add(DumpChecker(null, IrTextDumpHandler.DUMP_EXTENSION, "fir.${IrTextDumpHandler.DUMP_EXTENSION}"))
            add(DumpChecker(null, IrPrettyKotlinDumpHandler.DUMP_EXTENSION, "fir.${IrPrettyKotlinDumpHandler.DUMP_EXTENSION}"))
            for (externalClassId in testServices.moduleStructure.allDirectives[DUMP_EXTERNAL_CLASS]) {
                add(DumpChecker(".__${externalClassId.replace("/", ".")}", IrTextDumpHandler.DUMP_EXTENSION, "fir.${IrTextDumpHandler.DUMP_EXTENSION}"))
            }
        }
        if (failedAssertions.isNotEmpty()) return
        val testDataFile = testServices.moduleStructure.originalTestDataFiles.first()
        if (testServices.defaultsProvider.defaultFrontend != FrontendKinds.FIR)
            return
        if (DUMP_IR !in testServices.moduleStructure.allDirectives)
            return
        if (FIR_IDENTICAL in testServices.moduleStructure.allDirectives) {
            for (checker in dumpCheckers) {
                checker.deleteFirFile(testDataFile)
            }
            return
        }
        if (
            dumpCheckers.all { it.firAndClassicContentsAreEquals(testDataFile, trimLines = true) }
        ) {
            for (checker in dumpCheckers) {
                checker.deleteFirFile(testDataFile)
            }
            dumpCheckers.first().addDirectiveToClassicFileAndAssert(testDataFile)
        }
    }
}
