/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.frontend.fir

import org.jetbrains.kotlin.test.WrappedException
import org.jetbrains.kotlin.test.directives.FirDiagnosticsDirectives
import org.jetbrains.kotlin.test.frontend.fir.handlers.FirResolveContractViolationErrorHandler
import org.jetbrains.kotlin.test.model.AfterAnalysisChecker
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.moduleStructure
import org.jetbrains.kotlin.test.utils.firTestDataFile
import org.jetbrains.kotlin.test.utils.originalTestDataFile
import org.jetbrains.kotlin.test.utils.removeDirectiveFromFile


class DisableLazyResolveChecksAfterAnalysisChecker(
    testServices: TestServices
) : AfterAnalysisChecker(testServices) {
    companion object {
        private val isTeamCityBuild: Boolean = System.getenv("TEAMCITY_VERSION") != null
    }

    override fun check(failedAssertions: List<WrappedException>) {
        if (!isDisableLazyResolveDirectivePresent()) return
        if (failedAssertions.isNotEmpty()) return
        val testDataFile = testServices.moduleStructure.originalTestDataFiles.first()

        if (!isTeamCityBuild) {
            setOf(
                testDataFile.originalTestDataFile,
                testDataFile.firTestDataFile
            ).forEach { file ->
                file.removeDirectiveFromFile(FirDiagnosticsDirectives.FIR_DISABLE_LAZY_RESOLVE_CHECKS)
            }
        }

        val message = if (isTeamCityBuild) {
            "Please remove // ${FirDiagnosticsDirectives.FIR_DISABLE_LAZY_RESOLVE_CHECKS} from the test source"
        } else {
            "Removed // ${FirDiagnosticsDirectives.FIR_DISABLE_LAZY_RESOLVE_CHECKS} from the test source"
        }
        throw TestWithDisableLazyResolveDirectivePassesException(
            """
                    Lazy resolve contracts are satisfied now for this test and the test pass
                    $message
                    Please re-run the test now
                """.trimIndent()
        )
    }


    override fun suppressIfNeeded(failedAssertions: List<WrappedException>): List<WrappedException> {
        return if (isDisableLazyResolveDirectivePresent()) {
            failedAssertions.filterNot { it is WrappedException.FromHandler && it.handler is FirResolveContractViolationErrorHandler }
        } else {
            failedAssertions
        }
    }

    private fun isDisableLazyResolveDirectivePresent(): Boolean {
        val moduleStructure = testServices.moduleStructure
        return FirDiagnosticsDirectives.FIR_DISABLE_LAZY_RESOLVE_CHECKS in moduleStructure.allDirectives
    }
}

private class TestWithDisableLazyResolveDirectivePassesException(override val message: String) : IllegalStateException()
