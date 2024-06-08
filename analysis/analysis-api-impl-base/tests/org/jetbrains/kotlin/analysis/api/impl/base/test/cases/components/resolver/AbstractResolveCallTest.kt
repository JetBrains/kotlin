/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.resolver

import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.assertStableResult
import org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.stringRepresentation
import org.jetbrains.kotlin.analysis.api.resolution.KaCall
import org.jetbrains.kotlin.analysis.api.resolution.KaCallInfo
import org.jetbrains.kotlin.analysis.api.resolution.KaCallResolutionError
import org.jetbrains.kotlin.analysis.api.resolution.KaErrorCallInfo
import org.jetbrains.kotlin.analysis.api.resolution.KaSuccessCallInfo
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolution.KtResolvableCall
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.assertions

abstract class AbstractResolveCallTest : AbstractResolveByElementTest() {
    override val resolveKind: String get() = "call"

    override fun generateResolveOutput(mainElement: KtElement, testServices: TestServices): String = analyseForTest(mainElement) {
        val call = resolveCall(mainElement)
        val callInfo = call?.asCallInfo()

        val secondCall = resolveCall(mainElement)
        ignoreStabilityIfNeeded {
            assertStableResult(testServices, callInfo, secondCall?.asCallInfo())
        }

        // This call mustn't be suppressed as this is the API contracts
        assertSpecificResolutionApi(testServices, callInfo, mainElement)
        call?.let(::stringRepresentation) ?: "null"
    }

    private fun Any.asCallInfo(): KaCallInfo? = when (this) {
        is KaCallInfo -> this
        is KaCall -> KaSuccessCallInfo(this)
        is KaCallResolutionError -> KaErrorCallInfo(candidateCalls, diagnostic)
        else -> error("Unknown type: ${this::class.simpleName}")
    }

    private fun KaSession.resolveCall(element: KtElement): Any? = if (element is KtResolvableCall) {
        element.attemptResolveCall()
    } else {
        element.resolveCallOld()
    }

    private fun KaSession.assertSpecificResolutionApi(
        testServices: TestServices,
        callInfo: KaCallInfo?,
        element: KtElement,
    ) {
        if (element !is KtResolvableCall) return

        val specificCall = when (element) {
            is KtAnnotationEntry -> element.resolveCall()
            is KtSuperTypeCallEntry -> element.resolveCall()
            is KtConstructorDelegationCall -> element.resolveCall()
            is KtCallExpression -> element.resolveCall()
            is KtCallableReferenceExpression -> element.resolveCall()
            else -> return
        }

        val assertions = testServices.assertions
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
