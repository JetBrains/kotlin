/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.services.fir

import org.jetbrains.kotlin.test.WrappedException
import org.jetbrains.kotlin.test.directives.FirDiagnosticsDirectives
import org.jetbrains.kotlin.test.directives.FirDiagnosticsDirectives.SUPPRESS_NO_TYPE_ALIAS_EXPANSION_MODE
import org.jetbrains.kotlin.test.directives.model.DirectivesContainer
import org.jetbrains.kotlin.test.model.AfterAnalysisChecker
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.assertions
import org.jetbrains.kotlin.test.services.moduleStructure

class FirWithoutAliasExpansionTestSuppressor(testServices: TestServices) : AfterAnalysisChecker(testServices) {
    override val directiveContainers: List<DirectivesContainer>
        get() = listOf(FirDiagnosticsDirectives)

    override fun suppressIfNeeded(failedAssertions: List<WrappedException>): List<WrappedException> {
        if (SUPPRESS_NO_TYPE_ALIAS_EXPANSION_MODE !in testServices.moduleStructure.allDirectives) return failedAssertions

        return when {
            failedAssertions.isEmpty() -> testServices.assertions.fail {
                "Test is passing. Remove $SUPPRESS_NO_TYPE_ALIAS_EXPANSION_MODE directive"
            }

            else -> emptyList()
        }
    }
}
