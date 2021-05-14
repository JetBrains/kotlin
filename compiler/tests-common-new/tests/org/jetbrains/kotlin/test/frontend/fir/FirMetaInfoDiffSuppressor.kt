/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.frontend.fir

import org.jetbrains.kotlin.test.WrappedException
import org.jetbrains.kotlin.test.directives.CodegenTestDirectives
import org.jetbrains.kotlin.test.directives.CodegenTestDirectives.IGNORE_FIR_DIAGNOSTICS
import org.jetbrains.kotlin.test.directives.CodegenTestDirectives.IGNORE_FIR_DIAGNOSTICS_DIFF
import org.jetbrains.kotlin.test.directives.model.DirectivesContainer
import org.jetbrains.kotlin.test.model.AfterAnalysisChecker
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.moduleStructure

class FirMetaInfoDiffSuppressor(testServices: TestServices) : AfterAnalysisChecker(testServices) {
    override val directives: List<DirectivesContainer>
        get() = listOf(CodegenTestDirectives)

    override fun suppressIfNeeded(failedAssertions: List<WrappedException>): List<WrappedException> {
        val ignoreErrors = testServices.moduleStructure.modules.any { IGNORE_FIR_DIAGNOSTICS in it.directives }
        val ignoreDiff = testServices.moduleStructure.modules.any { IGNORE_FIR_DIAGNOSTICS_DIFF in it.directives }
        if (!(ignoreErrors || ignoreDiff)) {
            return failedAssertions
        }
        val filteredAssertions = failedAssertions.filterNot { it is WrappedException.FromMetaInfoHandler }
        return if (failedAssertions.size == filteredAssertions.size && ignoreDiff) {
            failedAssertions + AssertionError(
                "Test contains $IGNORE_FIR_DIAGNOSTICS_DIFF directive but no errors was reported. Please remove directive"
            ).wrap()
        } else {
            filteredAssertions
        }
    }
}
