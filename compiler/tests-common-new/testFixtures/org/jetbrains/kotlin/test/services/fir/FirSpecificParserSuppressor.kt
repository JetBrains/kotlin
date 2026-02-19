/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.services.fir

import org.jetbrains.kotlin.test.directives.FirDiagnosticsDirectives
import org.jetbrains.kotlin.test.directives.FirDiagnosticsDirectives.DISABLE_WITH_PARSER
import org.jetbrains.kotlin.test.directives.FirDiagnosticsDirectives.FIR_PARSER
import org.jetbrains.kotlin.test.directives.model.DirectivesContainer
import org.jetbrains.kotlin.test.directives.model.singleOrZeroValue
import org.jetbrains.kotlin.test.services.MetaTestConfigurator
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.moduleStructure

class FirSpecificParserSuppressor(testServices: TestServices) : MetaTestConfigurator(testServices) {
    override val directiveContainers: List<DirectivesContainer>
        get() = listOf(FirDiagnosticsDirectives)

    override fun shouldSkipTest(): Boolean {
        val directives = testServices.moduleStructure.allDirectives
        val suppressedParser = directives.singleOrZeroValue(DISABLE_WITH_PARSER) ?: return false
        val currentParser = directives.singleOrZeroValue(FIR_PARSER) ?: return false
        return currentParser == suppressedParser
    }
}
