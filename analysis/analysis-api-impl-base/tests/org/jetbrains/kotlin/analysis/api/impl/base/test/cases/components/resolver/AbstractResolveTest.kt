/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.resolver

import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.calls.KtCallCandidateInfo
import org.jetbrains.kotlin.analysis.api.calls.KtCallableMemberCall
import org.jetbrains.kotlin.analysis.api.calls.successfulCallOrNull
import org.jetbrains.kotlin.analysis.api.calls.symbol
import org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.compareCalls
import org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.stringRepresentation
import org.jetbrains.kotlin.analysis.test.framework.base.AbstractAnalysisApiBasedTest
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.KtResolvable
import org.jetbrains.kotlin.resolve.KtResolvableCall
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.assertions

abstract class AbstractResolveTest : AbstractAnalysisApiBasedTest() {
    protected fun processElement(
        element: KtElement,
        testServices: TestServices,
        renderedSymbol: (String) -> Unit,
        renderedCall: (String) -> Unit,
        renderedCandidates: (String) -> Unit,
    ) {
        analyseForTest(element) {
            val callInfo = element.resolveCallOld()
            val expectedSymbol = callInfo?.successfulCallOrNull<KtCallableMemberCall<*, *>>()?.symbol

            if (element is KtResolvable) {
                val actualSymbol = element.resolveSymbol()
                val actual = stringRepresentation(actualSymbol)
                renderedSymbol(actual)
                testServices.assertions.assertEquals(expectedSymbol, actualSymbol)
            }

            if (element is KtResolvableCall) {
                val attempt = element.attemptResolveCall()
                val actual = stringRepresentation(attempt)
                renderedCall(actual)
                testServices.assertions.assertEquals(expectedSymbol, (attempt as? KtCallableMemberCall<*, *>)?.symbol)

                val candidates = element.collectCallCandidates()
                val candidatesActual = renderCandidates(candidates)
                renderedCandidates(candidatesActual)
            }
        }
    }

    protected fun KtAnalysisSession.renderCandidates(candidates: List<KtCallCandidateInfo>): String {
        candidates.ifEmpty { return "NO_CANDIDATES" }

        val sortedCandidates = candidates.sortedWith { candidate1, candidate2 ->
            compareCalls(candidate1.candidate, candidate2.candidate)
        }

        return sortedCandidates.joinToString("\n\n") { stringRepresentation(it) }
    }

    protected val KtElement.elementToResolve: KtElement
        get() = when (this) {
            is KtValueArgument -> getArgumentExpression()!!
            is KtDeclarationModifierList -> annotationEntries.singleOrNull() ?: error("Only single annotation entry is supported for now")
            is KtFileAnnotationList -> annotationEntries.singleOrNull() ?: error("Only single annotation entry is supported for now")
            else -> this
        }
}
