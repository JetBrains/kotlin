/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.frontend.fir.handlers

import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.utils.isFirTestData
import org.jetbrains.kotlin.test.utils.isLLFirTestData
import java.io.File

class FirIdenticalChecker(testServices: TestServices) : AbstractFirIdenticalChecker(testServices) {
    override fun checkTestDataFile(testDataFile: File) {
        // Skip `.ll.kt` test files, which are instead checked by `LLFirIdenticalChecker`.
        if (testDataFile.isLLFirTestData) return

        if (testDataFile.isFirTestData) {
            val classicFile = helper.getClassicFileToCompare(testDataFile)
            if (".reversed." in classicFile.path) return

            if (helper.contentsAreEquals(classicFile, testDataFile, trimLines = true)) {
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
