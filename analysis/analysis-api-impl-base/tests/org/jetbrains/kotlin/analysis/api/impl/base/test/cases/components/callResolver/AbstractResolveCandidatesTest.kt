/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.callResolver

import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.calls.KaCallCandidateInfo
import org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.assertStableSymbolResult
import org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.compareCalls
import org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.stringRepresentation
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.moduleStructure

abstract class AbstractResolveCandidatesTest : AbstractResolveTest() {
    override val resolveKind: String get() = "candidates"

    override fun generateResolveOutput(mainElement: KtElement, testServices: TestServices): String {
        return analyseForTest(mainElement) {
            val candidates = collectCallCandidates(mainElement)
            ignoreStabilityIfNeeded(testServices.moduleStructure.allDirectives) {
                val candidatesAgain = collectCallCandidates(mainElement)
                assertStableSymbolResult(testServices, candidates, candidatesAgain)
            }

            if (candidates.isEmpty()) {
                "NO_CANDIDATES"
            } else {
                candidates.joinToString("\n\n") { stringRepresentation(it) }
            }
        }
    }

    private fun KaSession.collectCallCandidates(element: KtElement): List<KaCallCandidateInfo> {
        val candidates = element.collectCallCandidates()
        return candidates.sortedWith { candidate1, candidate2 ->
            compareCalls(candidate1.candidate, candidate2.candidate)
        }
    }
}
