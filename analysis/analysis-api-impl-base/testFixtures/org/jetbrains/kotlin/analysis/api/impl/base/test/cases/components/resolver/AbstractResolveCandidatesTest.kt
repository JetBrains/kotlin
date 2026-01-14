/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.resolver

import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.components.collectCallCandidates
import org.jetbrains.kotlin.analysis.api.components.resolveToCallCandidates
import org.jetbrains.kotlin.analysis.api.impl.base.components.asKaCallCandidates
import org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.assertStableSymbolResult
import org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.compareCalls
import org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.stringRepresentation
import org.jetbrains.kotlin.analysis.api.resolution.*
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtExperimentalApi
import org.jetbrains.kotlin.resolution.KtResolvableCall
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.assertions

abstract class AbstractResolveCandidatesTest : AbstractResolveByElementTest() {
    override val resolveKind: String get() = "candidates"

    @OptIn(KtExperimentalApi::class)
    override fun generateResolveOutput(mainElement: KtElement, testServices: TestServices): String = analyzeForTest(mainElement) {
        val candidates = collectCallCandidates(mainElement)
        val candidatesAgain = collectCallCandidates(mainElement)
        val callInfo = mainElement.resolveToCall()

        ignoreStabilityIfNeeded {
            assertStableSymbolResult(testServices, candidates.asKaCallCandidates(), candidatesAgain.asKaCallCandidates())
            checkConsistencyWithResolveCall(callInfo, candidates.asKaCallCandidates(), testServices)
        }

        val sortedCandidates = sortCandidates(candidates)
        if (sortedCandidates.isEmpty()) {
            "NO_CANDIDATES"
        } else {
            sortedCandidates.joinToString("\n\n") { stringRepresentation(it) }
        }
    }

    context(_: KaSession)
    private fun checkConsistencyWithResolveCall(
        callInfo: KaCallInfo?,
        candidates: List<KaCallCandidate>,
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
                    stringRepresentation(it.candidate.signature.symbol)
                } else {
                    null
                }
            }

            testServices.assertions.assertTrue(resolvedSymbol in candidatesRepresentation) {
                "'$resolvedSymbol' is not found in:\n" + candidatesRepresentation.joinToString("\n")
            }
        }
    }

    /**
     * Returns either [List]<[KaCallCandidate]> (new API) or [List]<[KaCallCandidateInfo]> (old API).
     */
    @OptIn(KtExperimentalApi::class)
    context(_: KaSession)
    private fun collectCallCandidates(element: KtElement): List<*> = if (element is KtResolvableCall) {
        element.collectCallCandidates()
    } else {
        element.resolveToCallCandidates()
    }

    /**
     * Converts to [List]<[KaCallCandidate]> for consistency checking.
     */
    @Suppress("UNCHECKED_CAST")
    private fun List<*>.asKaCallCandidates(): List<KaCallCandidate> = when (val first = firstOrNull()) {
        null -> emptyList()
        is KaCallCandidate -> this as List<KaCallCandidate>
        is KaCallCandidateInfo -> (this as List<KaCallCandidateInfo>).flatMap(KaCallCandidateInfo::asKaCallCandidates)
        else -> error("Unknown type: ${first::class.simpleName}")
    }

    context(_: KaSession)
    private fun sortCandidates(candidates: List<*>): List<*> = candidates.sortedWith { a, b ->
        val call1 = when (a) {
            is KaCallCandidate -> a.candidate as KaCall
            is KaCallCandidateInfo -> a.candidate
            else -> return@sortedWith 0
        }

        val call2 = when (b) {
            is KaCallCandidate -> b.candidate as KaCall
            is KaCallCandidateInfo -> b.candidate
            else -> return@sortedWith 0
        }

        compareCalls(call1, call2)
    }
}
