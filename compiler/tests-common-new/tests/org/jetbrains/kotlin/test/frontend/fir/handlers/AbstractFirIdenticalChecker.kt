/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
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
}
