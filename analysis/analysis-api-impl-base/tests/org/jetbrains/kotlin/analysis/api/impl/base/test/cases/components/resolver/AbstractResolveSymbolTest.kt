/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.resolver

import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.impl.base.resolution.KaSymbolResolutionSuccessImpl
import org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.assertStableResult
import org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.stringRepresentation
import org.jetbrains.kotlin.analysis.api.resolution.KaSymbolResolutionAttempt
import org.jetbrains.kotlin.analysis.api.resolution.KaSymbolResolutionError
import org.jetbrains.kotlin.analysis.api.resolution.KaSymbolResolutionSuccess
import org.jetbrains.kotlin.psi.KtAnnotationEntry
import org.jetbrains.kotlin.psi.KtArrayAccessExpression
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtCallableReferenceExpression
import org.jetbrains.kotlin.psi.KtCollectionLiteralExpression
import org.jetbrains.kotlin.psi.KtConstructorDelegationCall
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtSuperTypeCallEntry
import org.jetbrains.kotlin.resolution.KtResolvable
import org.jetbrains.kotlin.resolution.KtResolvableCall
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.assertions

abstract class AbstractResolveSymbolTest : AbstractResolveByElementTest() {
    override val resolveKind: String get() = "symbol"

    override fun generateResolveOutput(element: KtElement, testServices: TestServices): String = analyseForTest(element) {
        val symbolAttempt = attemptResolveSymbol(element)
        val secondSymbolAttempt = attemptResolveSymbol(element)

        ignoreStabilityIfNeeded {
            assertStableResult(testServices, symbolAttempt, secondSymbolAttempt)

            if (element is KtResolvableCall) {
                val callAttempt = element.attemptResolveCall()
                assertStableResult(testServices, symbolAttempt, callAttempt)
            }
        }

        // This call mustn't be suppressed as this is the API contracts
        assertSpecificResolutionApi(testServices, symbolAttempt, element)
        symbolAttempt?.let(::stringRepresentation) ?: "null"
    }

    private fun KaSession.assertSpecificResolutionApi(
        testServices: TestServices,
        symbolAttempt: KaSymbolResolutionAttempt?,
        element: KtElement,
    ) {
        if (element !is KtResolvable) return

        val specificSymbol = when (element) {
            is KtAnnotationEntry -> element.resolveSymbol()
            is KtSuperTypeCallEntry -> element.resolveSymbol()
            is KtConstructorDelegationCall -> element.resolveSymbol()
            is KtCallExpression -> element.resolveSymbol()
            is KtCallableReferenceExpression -> element.resolveSymbol()
            is KtArrayAccessExpression -> element.resolveSymbol()
            is KtCollectionLiteralExpression -> element.resolveSymbol()
            else -> return
        }

        val assertions = testServices.assertions
        when (symbolAttempt) {
            null, is KaSymbolResolutionError -> assertions.assertEquals(expected = null, actual = specificSymbol)
            is KaSymbolResolutionSuccess -> assertStableResult(
                testServices = testServices,
                firstAttempt = symbolAttempt,
                secondAttempt = specificSymbol?.let(::KaSymbolResolutionSuccessImpl),
            )
        }
    }

    private fun KaSession.attemptResolveSymbol(element: KtElement): KaSymbolResolutionAttempt? = if (element is KtResolvable) {
        element.attemptResolveSymbol()
    } else {
        null
    }
}
