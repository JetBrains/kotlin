/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.frontend.fir.handlers

import org.jetbrains.kotlin.test.WrappedException
import org.jetbrains.kotlin.test.model.AfterAnalysisChecker
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.moduleStructure
import org.jetbrains.kotlin.test.utils.FirIdenticalCheckerHelper
import org.jetbrains.kotlin.test.utils.firTestDataFile
import org.jetbrains.kotlin.test.utils.isFirTestData
import org.jetbrains.kotlin.test.utils.originalTestDataFile
import java.io.File

class FirIdenticalChecker(testServices: TestServices) : AfterAnalysisChecker(testServices) {
    private val helper = object : FirIdenticalCheckerHelper(testServices) {
        override fun getClassicFileToCompare(testDataFile: File): File {
            return if (testDataFile.isFirTestData) testDataFile.originalTestDataFile else testDataFile
        }

        override fun getFirFileToCompare(testDataFile: File): File {
            return if (testDataFile.isFirTestData) testDataFile else testDataFile.firTestDataFile
        }
    }

    override fun check(failedAssertions: List<WrappedException>) {
        if (failedAssertions.isNotEmpty()) return
        val testDataFile = testServices.moduleStructure.originalTestDataFiles.first()
        if (testDataFile.isFirTestData) {
            val firFile = helper.getFirFileToCompare(testDataFile)
            val classicFile = helper.getClassicFileToCompare(testDataFile)
            if (helper.contentsAreEquals(classicFile, firFile, trimLines = true)) {
                helper.deleteFirFile(testDataFile)
                helper.addDirectiveToClassicFileAndAssert(classicFile)
            }
        } else {
            removeFirFileIfExist(testDataFile)
        }
    }

    private fun removeFirFileIfExist(testDataFile: File) {
        val firFile = helper.getFirFileToCompare(testDataFile)
        firFile.delete()
    }
}
