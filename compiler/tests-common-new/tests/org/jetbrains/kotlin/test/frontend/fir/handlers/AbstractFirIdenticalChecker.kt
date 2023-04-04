/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.frontend.fir.handlers

import org.jetbrains.kotlin.test.WrappedException
import org.jetbrains.kotlin.test.model.AfterAnalysisChecker
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.moduleStructure
import org.jetbrains.kotlin.test.utils.FirIdenticalCheckerHelper
import org.jetbrains.kotlin.test.utils.firTestDataFile
import org.jetbrains.kotlin.test.utils.originalTestDataFile
import java.io.File
import org.jetbrains.kotlin.test.directives.model.RegisteredDirectives
import org.jetbrains.kotlin.test.model.TestFile
import org.jetbrains.kotlin.test.services.assertions
import org.jetbrains.kotlin.test.services.sourceFileProvider

abstract class AbstractFirIdenticalChecker(testServices: TestServices) : AfterAnalysisChecker(testServices) {
    protected inner class SpecificHelper : FirIdenticalCheckerHelper(testServices) {
        override fun getClassicFileToCompare(testDataFile: File): File = testDataFile.originalTestDataFile
        override fun getFirFileToCompare(testDataFile: File): File = testDataFile.firTestDataFile
    }

    protected val helper = SpecificHelper()

    protected abstract fun checkTestDataFile(testDataFile: File)

    final override fun check(failedAssertions: List<WrappedException>) {
        if (failedAssertions.isNotEmpty()) return
        val testDataFile = testServices.moduleStructure.originalTestDataFiles.first()
        checkTestDataFile(testDataFile)
    }

    /**
     * Asserts that [baseFile] and [customFile] have the same content after preprocessing (which removes diagnostics and other meta info). This
     * prevents situations where one test data changes, but changes to the other test data are forgotten.
     */
    protected fun assertPreprocessedTestDataAreEqual(
        testServices: TestServices,
        baseFile: File,
        baseContent: String,
        customFile: File,
        customContent: String,
        message: () -> String,
    ) {
        val processedBaseContent = testServices.sourceFileProvider.getContentOfSourceFile(
            TestFile(
                baseFile.path,
                baseContent,
                baseFile,
                startLineNumberInOriginalFile = 0,
                isAdditional = false,
                RegisteredDirectives.Empty,
            )
        )
        val processedLlContent = testServices.sourceFileProvider.getContentOfSourceFile(
            TestFile(
                customFile.path,
                customContent,
                customFile,
                startLineNumberInOriginalFile = 0,
                isAdditional = false,
                RegisteredDirectives.Empty,
            )
        )

        testServices.assertions.assertEquals(processedBaseContent, processedLlContent, message)
    }
}
