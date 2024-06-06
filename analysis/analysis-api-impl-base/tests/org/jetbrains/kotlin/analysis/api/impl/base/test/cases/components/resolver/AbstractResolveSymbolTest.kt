/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.resolver

import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.assertStableResult
import org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.stringRepresentation
import org.jetbrains.kotlin.analysis.api.resolution.KaSymbolResolutionAttempt
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.resolution.KtResolvable
import org.jetbrains.kotlin.resolution.KtResolvableCall
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.moduleStructure

abstract class AbstractResolveSymbolTest : AbstractResolveByElementTest() {
    override val resolveKind: String get() = "symbol"

    override fun generateResolveOutput(element: KtElement, testServices: TestServices): String = analyseForTest(element) {
        val symbolAttempt = attemptResolveSymbol(element)
        val secondSymbolAttempt = attemptResolveSymbol(element)

        ignoreStabilityIfNeeded(testServices.moduleStructure.allDirectives) {
            assertStableResult(testServices, symbolAttempt, secondSymbolAttempt)

            if (element is KtResolvableCall) {
                val callAttempt = element.attemptResolveCall()
                assertStableResult(testServices, symbolAttempt, callAttempt)
            }
        }

        symbolAttempt?.let(::stringRepresentation) ?: "null"
    }

    private fun KaSession.attemptResolveSymbol(element: KtElement): KaSymbolResolutionAttempt? = if (element is KtResolvable) {
        element.attemptResolveSymbol()
    } else {
        null
    }
}
