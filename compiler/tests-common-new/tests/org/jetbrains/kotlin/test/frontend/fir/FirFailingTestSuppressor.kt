/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.frontend.fir

import org.jetbrains.kotlin.test.WrappedException
import org.jetbrains.kotlin.test.model.AfterAnalysisChecker
import org.jetbrains.kotlin.test.model.FrontendKinds
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.moduleStructure
import org.jetbrains.kotlin.test.utils.firTestDataFile

class FirFailingTestSuppressor(testServices: TestServices) : AfterAnalysisChecker(testServices) {
    override fun suppressIfNeeded(failedAssertions: List<WrappedException>): List<WrappedException> {
        val testFile = testServices.moduleStructure.originalTestDataFiles.first().firTestDataFile
        val failFile = testFile.parentFile.resolve("${testFile.nameWithoutExtension}.fail").takeIf { it.exists() }
            ?: return failedAssertions
        val failReason = failFile.readText().trim()
        val hasFail = failedAssertions.any {
            when (it) {
                is WrappedException.FromFacade -> it.facade is FirFrontendFacade
                is WrappedException.FromHandler -> it.handler.artifactKind == FrontendKinds.FIR
                else -> false
            }
        }
        if (hasFail || failReason == INCONSISTENT_DIAGNOSTICS) return emptyList()
        return failedAssertions + AssertionError("Fail file exists but no exception was thrown. Please remove ${failFile.name}").wrap()
    }

    companion object {
        const val INCONSISTENT_DIAGNOSTICS = "INCONSISTENT_DIAGNOSTICS"
    }
}
