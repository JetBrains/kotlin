/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.resolver

import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.assertStableResult
import org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.stringRepresentation
import org.jetbrains.kotlin.analysis.api.resolution.KaSymbolResolutionAttempt
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtExperimentalApi
import org.jetbrains.kotlin.resolution.KtResolvable
import org.jetbrains.kotlin.resolution.KtResolvableCall
import org.jetbrains.kotlin.test.services.TestServices

@OptIn(KtExperimentalApi::class)
abstract class AbstractResolveSymbolTest : AbstractResolveByElementTest() {
    override val resolveKind: String get() = "symbol"

    override fun generateResolveOutput(mainElement: KtElement, testServices: TestServices): String = analyzeForTest(mainElement) {
        val symbolAttempt = tryResolveSymbol(mainElement)
        val secondSymbolAttempt = tryResolveSymbol(mainElement)

        ignoreStabilityIfNeeded {
            assertStableResult(testServices, symbolAttempt, secondSymbolAttempt)

            if (mainElement is KtResolvableCall) {
                val callAttempt = mainElement.tryResolveCall()
                assertStableResult(testServices, symbolAttempt, callAttempt)
            }
        }

        stringRepresentation(symbolAttempt)
    }

    private fun KaSession.tryResolveSymbol(element: KtElement): KaSymbolResolutionAttempt? = if (element is KtResolvable) {
        element.tryResolveSymbol()
    } else {
        null
    }
}
