/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.services.fir

import org.jetbrains.kotlin.test.directives.FirDiagnosticsDirectives
import org.jetbrains.kotlin.test.directives.FirDiagnosticsDirectives.SUPPRESS_NO_TYPE_ALIAS_EXPANSION_MODE
import org.jetbrains.kotlin.test.model.TestFailureSuppressorBySingleDirective
import org.jetbrains.kotlin.test.services.MetaTestConfigurator
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.moduleStructure

class FirWithoutAliasExpansionTestSuppressor(testServices: TestServices) : TestFailureSuppressorBySingleDirective(
    suppressDirective = SUPPRESS_NO_TYPE_ALIAS_EXPANSION_MODE,
    directivesContainer = FirDiagnosticsDirectives,
    testServices,
    order = Order.P5,
)

class OnlyTestsWithTypeAliasesMetaConfigurator(testServices: TestServices) : MetaTestConfigurator(testServices) {
    override fun shouldSkipTest(): Boolean {
        val testText = testServices.moduleStructure.originalTestDataFiles.first().readText()
        return !testText.contains("typealias")
    }
}
