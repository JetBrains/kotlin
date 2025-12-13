/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.resolver

import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.components.KaResolver
import org.jetbrains.kotlin.analysis.api.components.resolveToCall
import org.jetbrains.kotlin.analysis.api.components.tryResolveCall
import org.jetbrains.kotlin.analysis.api.impl.base.resolution.KaBaseErrorCallInfo
import org.jetbrains.kotlin.analysis.api.impl.base.resolution.KaBaseSuccessCallInfo
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
        val call = tryResolveCall(mainElement)
        val callInfo = call?.asCallInfo()

        val secondCall = tryResolveCall(mainElement)

        ignoreStabilityIfNeeded {
            assertStableResult(testServices, callInfo, secondCall?.asCallInfo())
        }

        // This call mustn't be suppressed as this is the API contracts
        assertSpecificResolutionApi(testServices, callInfo, mainElement)
        stringRepresentation(call)
    }

    private fun Any.asCallInfo(): KaCallInfo? = when (this) {
        is KaCallInfo -> this
        is KaCall -> KaBaseSuccessCallInfo(this)
        is KaCallResolutionError -> KaBaseErrorCallInfo(candidateCalls, diagnostic)
        else -> error("Unknown type: ${this::class.simpleName}")
    }

    context(_: KaSession)
    private fun tryResolveCall(element: KtElement): Any? = if (element is KtResolvableCall) {
        element.tryResolveCall()
    } else {
        element.resolveToCall()
    }

    /**
     * The function checks that all specific implementations of [KaResolver.resolveCall] are consistent.
     */
    context(session: KaSession)
    private fun assertSpecificResolutionApi(
        testServices: TestServices,
        callInfo: KaCallInfo?,
        element: KtElement,
    ) {
        if (element !is KtResolvableCall) return
        val elementClass = element::class

        val assertions = testServices.assertions
        for (kFunction in KaResolver::class.findSpecializedResolveFunctions("resolveCall", elementClass)) {
            val specificCall = kFunction.call(session, element)

            when (callInfo) {
                null, is KaErrorCallInfo -> assertions.assertEquals(expected = null, actual = specificCall)
                is KaSuccessCallInfo -> assertStableResult(
                    testServices = testServices,
                    firstInfo = callInfo,
                    secondInfo = specificCall?.asCallInfo(),
                )
            }
        }
    }
}
