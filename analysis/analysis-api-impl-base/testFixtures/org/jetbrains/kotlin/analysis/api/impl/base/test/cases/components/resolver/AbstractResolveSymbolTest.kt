/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.resolver

import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.components.KaResolver
import org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.assertStableResult
import org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.findSpecializedResolveFunctions
import org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.stringRepresentation
import org.jetbrains.kotlin.analysis.api.resolution.KaSymbolResolutionAttempt
import org.jetbrains.kotlin.analysis.api.resolution.KaSymbolResolutionError
import org.jetbrains.kotlin.analysis.api.resolution.KaSymbolResolutionSuccess
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtExperimentalApi
import org.jetbrains.kotlin.resolution.KtResolvable
import org.jetbrains.kotlin.resolution.KtResolvableCall
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.assertions

@OptIn(KtExperimentalApi::class)
abstract class AbstractResolveSymbolTest : AbstractResolveByElementTest() {
    override val resolveKind: String get() = "symbol"

    override fun generateResolveOutput(mainElement: KtElement, testServices: TestServices): String = analyzeForTest(mainElement) {
        val symbolAttempt = tryResolveSymbols(mainElement)
        val secondSymbolAttempt = tryResolveSymbols(mainElement)

        ignoreStabilityIfNeeded {
            assertStableResult(testServices, symbolAttempt, secondSymbolAttempt)

            if (mainElement is KtResolvableCall) {
                val callAttempt = mainElement.tryResolveCall()
                assertStableResult(testServices, symbolAttempt, callAttempt)
            }
        }

        // This call mustn't be suppressed as this is the API contracts
        if (mainElement is KtResolvable) {
            assertSpecificResolutionApi(testServices, symbolAttempt, mainElement)
        }

        stringRepresentation(symbolAttempt)
    }

    private fun KaSession.tryResolveSymbols(element: KtElement): KaSymbolResolutionAttempt? = if (element is KtResolvable) {
        element.tryResolveSymbols()
    } else {
        null
    }
}

/**
 * The function checks that all specific implementations of [KaResolver.resolveSymbol] are consistent.
 */
@OptIn(KtExperimentalApi::class)
context(session: KaSession)
internal fun assertSpecificResolutionApi(
    testServices: TestServices,
    attempt: KaSymbolResolutionAttempt?,
    element: KtResolvable,
) {
    val elementClass = element::class

    val assertions = testServices.assertions
    for (kFunction in KaResolver::class.findSpecializedResolveFunctions("resolveSymbol", elementClass)) {
        val specificCall = kFunction.call(session, element)

        when (attempt) {
            null, is KaSymbolResolutionError -> assertions.assertEquals(expected = null, actual = specificCall)
            is KaSymbolResolutionSuccess -> {
                // Only non-compound cases can be checked
                assertions.assertEquals(expected = attempt.symbols.singleOrNull(), actual = specificCall)
            }
        }
    }
}