/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.frontend.fir.handlers

import org.jetbrains.kotlin.test.WrappedException
import org.jetbrains.kotlin.test.directives.model.RegisteredDirectives
import org.jetbrains.kotlin.test.model.AfterAnalysisChecker
import org.jetbrains.kotlin.test.model.TestFile
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.assertions
import org.jetbrains.kotlin.test.services.moduleStructure
import org.jetbrains.kotlin.test.services.sourceFileProvider
import java.io.File

/**
 * Some tests duplicate the original .kt file with different extension because they could report different diagnostic meta-info
 * in them (e.g. tests for AA or with the latest LV). These duplicated `.kt` files should have their content to be identical
 * to the base .kt file content (except the reported meta-infos).
 *
 * This class is a base for checkers which ensure that the duplicated `.kt` file content is identical to the base .kt file content.
 */
abstract class AbstractAlternativeKtFileIdenticalChecker(testServices: TestServices) : AfterAnalysisChecker(testServices) {
    protected abstract fun checkTestDataFile(testDataFile: File)

    override val order: Order
        get() = Order.P5

    /**
     * [org.jetbrains.kotlin.test.NonGroupingTestRunner] runs `check` for all checkers and then `suppressIfNeeded`
     * for all checkers. Since this checker relies on the fact that there are no other failures in the
     * test, we need to run it after all other suppressing checkers already suppressed all required
     * failures
     */
    final override fun check(failedAssertions: List<WrappedException>) {}

    final override fun suppressIfNeeded(failedAssertions: List<WrappedException>): List<WrappedException> {
        if (failedAssertions.isNotEmpty()) return failedAssertions
        val testDataFile = testServices.moduleStructure.originalTestDataFiles.first()
        return try {
            checkTestDataFile(testDataFile)
            emptyList()
        } catch (e: Throwable) {
            listOf(WrappedException.FromAfterAnalysisChecker(e))
        }
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
        ).replace("\r", "")
        val processedLlContent = testServices.sourceFileProvider.getContentOfSourceFile(
            TestFile(
                customFile.path,
                customContent,
                customFile,
                startLineNumberInOriginalFile = 0,
                isAdditional = false,
                RegisteredDirectives.Empty,
            )
        ).replace("\r", "")

        testServices.assertions.assertEquals(processedBaseContent, processedLlContent, message)
    }

    protected fun readContent(file: File, trimLines: Boolean): String {
        return if (trimLines) {
            file.readLines().joinToString("\n") { it.trimEnd() }.trim()
        } else {
            file.readText()
        }
    }
}
