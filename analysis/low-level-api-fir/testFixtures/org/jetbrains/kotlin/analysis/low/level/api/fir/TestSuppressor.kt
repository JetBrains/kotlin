/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir

import org.jetbrains.kotlin.test.directives.model.SimpleDirectivesContainer
import org.jetbrains.kotlin.test.model.TestFailureSuppressorBySingleDirective
import org.jetbrains.kotlin.test.services.TestServices

class LLFirTestSuppressor(
    testServices: TestServices,
) : TestFailureSuppressorBySingleDirective(
    suppressDirective = Directives.MUTE_LL_FIR,
    directivesContainer = Directives,
    testServices
) {

    private object Directives : SimpleDirectivesContainer() {
        val MUTE_LL_FIR by stringDirective("Temporary mute Low Level FIR implementation due to some error. YT ticket must be provided")
    }
}

class LLFirOnlyReversedTestSuppressor(
    testServices: TestServices,
) : TestFailureSuppressorBySingleDirective(
    suppressDirective = Directives.IGNORE_REVERSED_RESOLVE,
    directivesContainer = Directives,
    testServices
) {
    private object Directives : SimpleDirectivesContainer() {
        val IGNORE_REVERSED_RESOLVE by stringDirective("Temporary disables reversed resolve checks until the issue is fixed. YT ticket must be provided")
    }
}

class LLFirOnlyNonReversedTestSuppressor(
    testServices: TestServices,
) : TestFailureSuppressorBySingleDirective(
    suppressDirective = Directives.IGNORE_NON_REVERSED_RESOLVE,
    directivesContainer = Directives,
    testServices
) {
    private object Directives : SimpleDirectivesContainer() {
        val IGNORE_NON_REVERSED_RESOLVE by stringDirective("Temporary disables non-reversed resolve checks until the issue is fixed. YT ticket must be provided")
    }
}
