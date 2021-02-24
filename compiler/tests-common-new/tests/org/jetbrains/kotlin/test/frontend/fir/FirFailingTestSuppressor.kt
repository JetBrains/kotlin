/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.frontend.fir

import org.jetbrains.kotlin.test.ExceptionFromTestError
import org.jetbrains.kotlin.test.model.AfterAnalysisChecker
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.moduleStructure

class FirFailingTestSuppressor(testServices: TestServices) : AfterAnalysisChecker(testServices) {
    override fun suppressIfNeeded(failedAssertions: List<Throwable>): List<Throwable> {
        val testFile = testServices.moduleStructure.originalTestDataFiles.first()
        val failFile = testFile.parentFile.resolve("${testFile.nameWithoutExtension}.fail")
        val exceptionFromFir = failedAssertions.firstOrNull { it is ExceptionFromTestError }
        return when {
            failFile.exists() && exceptionFromFir != null -> emptyList()
            failFile.exists() && exceptionFromFir == null -> {
                failedAssertions + AssertionError("Fail file exists but no exception was throw. Please remove ${failFile.name}")
            }
            else -> failedAssertions
        }
    }
}
