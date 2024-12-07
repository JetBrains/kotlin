/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.resolver

import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.assertStableSymbolResult
import org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.compareCalls
import org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.stringRepresentation
import org.jetbrains.kotlin.analysis.api.resolution.KaCallCandidateInfo
import org.jetbrains.kotlin.analysis.api.resolution.KaCallInfo
import org.jetbrains.kotlin.analysis.api.resolution.KaCallableMemberCall
import org.jetbrains.kotlin.analysis.api.resolution.successfulCallOrNull
import org.jetbrains.kotlin.analysis.api.resolution.symbol
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.assertions

abstract class AbstractResolveCandidatesTest : AbstractResolveByElementTest() {
    override val resolveKind: String get() = "candidates"

    override fun generateResolveOutput(mainElement: KtElement, testServices: TestServices): String = analyseForTest(mainElement) {
        val candidates = collectCallCandidates(mainElement)
        val candidatesAgain = collectCallCandidates(mainElement)
        val callInfo = mainElement.resolveToCall()

        ignoreStabilityIfNeeded {
            assertStableSymbolResult(testServices, candidates, candidatesAgain)
            checkConsistencyWithResolveCall(callInfo, candidates, testServices)
        }

        if (candidates.isEmpty()) {
            "NO_CANDIDATES"
        } else {
            candidates.joinToString("\n\n") { stringRepresentation(it) }
        }
    }

    private fun KaSession.checkConsistencyWithResolveCall(
        callInfo: KaCallInfo?,
        candidates: List<KaCallCandidateInfo>,
        testServices: TestServices,
    ) {
        val resolvedCall = callInfo?.successfulCallOrNull<KaCallableMemberCall<*, *>>()
        if (candidates.isEmpty()) {
            testServices.assertions.assertEquals(null, resolvedCall) {
                "Inconsistency between candidates and resolved call. " +
                        "Resolved call is not null, but no candidates found.\n" +
                        stringRepresentation(resolvedCall)
            }
        } else {
            if (resolvedCall == null) return
            val resolvedSymbol = stringRepresentation(resolvedCall.symbol)
            val candidatesRepresentation = candidates.mapNotNull {
                if (it.isInBestCandidates) {
                    stringRepresentation((it.candidate as KaCallableMemberCall<*, *>).symbol)
                } else {
                    null
                }
            }

            testServices.assertions.assertTrue(resolvedSymbol in candidatesRepresentation) {
                "'$resolvedSymbol' is not found in:\n" + candidatesRepresentation.joinToString("\n")
            }
        }
    }

    private fun KaSession.collectCallCandidates(element: KtElement): List<KaCallCandidateInfo> {
        val candidates = element.resolveToCallCandidates()
        return candidates.sortedWith { candidate1, candidate2 ->
            compareCalls(candidate1.candidate, candidate2.candidate)
        }
    }
}
