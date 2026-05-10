/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.resolver

import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.components.KaResolver
import org.jetbrains.kotlin.analysis.api.components.resolveToCall
import org.jetbrains.kotlin.analysis.api.components.tryResolveCall
import org.jetbrains.kotlin.analysis.api.impl.base.resolution.KaBaseCallResolutionError
import org.jetbrains.kotlin.analysis.api.impl.base.resolution.KaBaseCallResolutionSuccess
import org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.assertStableResult
import org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.findSpecializedResolveFunctions
import org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.stringRepresentation
import org.jetbrains.kotlin.analysis.api.resolution.*
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtExperimentalApi
import org.jetbrains.kotlin.resolution.KtResolvableCall
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.assertions

@OptIn(KtExperimentalApi::class)
abstract class AbstractResolveCallTest : AbstractResolveByElementTest() {
    override val resolveKind: String get() = "call"

    override fun generateResolveOutput(mainElement: KtElement, testServices: TestServices): String = analyzeForTest(mainElement) {
        val result = tryResolveCall(mainElement)
        val attempt = result?.asCallResolutionAttempt()
        val secondAttempt = tryResolveCall(mainElement)?.asCallResolutionAttempt()

        ignoreStabilityIfNeeded {
            assertStableResult(testServices, attempt, secondAttempt)
            if (mainElement is KtResolvableCall) {
                val oldAttempt = mainElement.resolveToCall()?.asCallResolutionAttempt()
                // Old API collapses multi-calls into a single success/error, losing some symbols.
                // We only check that old API symbols are a subset of new API symbols.
                val newSymbols = attempt?.calls?.flatMap(KaSingleOrMultiCall::symbols)
                    ?.map { stringRepresentation(it) }?.toSet().orEmpty()
                val oldSymbols = oldAttempt?.calls?.flatMap(KaSingleOrMultiCall::symbols)
                    ?.map { stringRepresentation(it) }?.toSet().orEmpty()

                testServices.assertions.assertTrue(newSymbols.containsAll(oldSymbols)) {
                    "Old API symbols not found in new API:\nold: $oldSymbols\nnew: $newSymbols"
                }
            }
        }

        // This call mustn't be suppressed as this is the API contracts
        assertSpecificResolutionApi(testServices, attempt, mainElement)

        stringRepresentation(result)
    }

    context(_: KaSession)
    private fun tryResolveCall(element: KtElement): Any? = if (element is KtResolvableCall) {
        element.tryResolveCall()
    } else {
        element.resolveToCall()
    }

    private fun Any.asCallResolutionAttempt(): KaCallResolutionAttempt = when (this) {
        is KaCallResolutionAttempt -> this
        is KaSuccessCallInfo -> {
            val singleCall = (call as KaSingleOrMultiCall).calls.first()
            KaBaseCallResolutionSuccess(singleCall)
        }

        is KaErrorCallInfo -> KaBaseCallResolutionError(
            backedDiagnostic = diagnostic,
            backingCandidateCalls = candidateCalls.flatMap {
                (it as KaSingleOrMultiCall).calls
            },
        )

        else -> error("Unknown type: ${this::class.simpleName}")
    }

    /**
     * The function checks that all specific implementations of [KaResolver.resolveCall] and [KaResolver.tryResolveCall] are consistent.
     */
    context(session: KaSession)
    private fun assertSpecificResolutionApi(
        testServices: TestServices,
        attempt: KaCallResolutionAttempt?,
        element: KtElement,
    ) {
        if (element !is KtResolvableCall) return
        val elementClass = element::class

        val assertions = testServices.assertions
        val expectedCall = attempt?.successfulCall
        for (kFunction in KaResolver::class.findSpecializedResolveFunctions("resolveCall", elementClass)) {
            val specificCall = kFunction.call(session, element) as? KaSingleOrMultiCall
            if (expectedCall == null || specificCall == null) {
                assertions.assertEquals(expected = expectedCall, actual = specificCall)
            } else {
                assertStableResult(testServices, expectedCall, specificCall)
            }
        }

        for (kFunction in KaResolver::class.findSpecializedResolveFunctions("tryResolveCall", elementClass)) {
            val specificAttempt = kFunction.call(session, element) as? KaCallResolutionAttempt
            assertStableResult(testServices, attempt, specificAttempt)
        }
    }
}
