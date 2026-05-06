/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.compiler.based

import org.jetbrains.kotlin.analysis.low.level.api.fir.LLResolutionFacadeService
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.getOrBuildFirFile
import org.jetbrains.kotlin.analysis.test.framework.projectStructure.ktTestModuleStructure
import org.jetbrains.kotlin.test.frontend.fir.checkDistinctSourceElements
import org.jetbrains.kotlin.test.model.AfterAnalysisChecker
import org.jetbrains.kotlin.test.services.TestServices

/**
 * For all compiler-based tests, this checker ensures that the source elements of FIR declarations are distinct after analysis. See
 * [checkDistinctSourceElements] for more information.
 *
 * FIR files are checked in the state they were resolved to, or built fresh and checked as raw FIR if they haven't been resolved yet.
 *
 * See [FirDistinctSourceElementsHandler][org.jetbrains.kotlin.test.frontend.fir.handlers.FirDistinctSourceElementsHandler] for the
 * corresponding compiler checker.
 */
class LLDistinctSourceElementsChecker(testServices: TestServices) : AfterAnalysisChecker(testServices) {
    override fun check(thereWereFailures: Boolean) {
        // We ignore failed assertions. With symbol IDs, duplicate source elements can easily lead to resolution problems, so they have a
        // higher priority than the resolution test failure itself.

        val project = testServices.ktTestModuleStructure.project
        for (testModule in testServices.ktTestModuleStructure.mainModules) {
            val resolutionFacade = LLResolutionFacadeService.getInstance(project).getResolutionFacade(testModule.ktModule)
            val firFiles = testModule.ktFiles.map { it.getOrBuildFirFile(resolutionFacade) }

            checkDistinctSourceElements(firFiles) { _, _ -> "Duplicate source elements" }
        }
    }
}
