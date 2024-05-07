/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.callResolver

import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.calls.KtCallInfo
import org.jetbrains.kotlin.analysis.api.calls.calls
import org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.sortedCalls
import org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.stringRepresentation
import org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.symbols
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.assertions

abstract class AbstractResolveCallTest : AbstractResolveTest() {
    override fun doResolutionTest(mainElement: KtElement, testServices: TestServices) {
        val actual = analyseForTest(mainElement) {
            val call = mainElement.resolveCall()
            val secondCall = mainElement.resolveCall()
            assertStableSymbolResult(testServices, call, secondCall)

            call?.let(::stringRepresentation) ?: "null"
        }

        testServices.assertions.assertEqualsToTestDataFileSibling(actual)
    }

    fun KtAnalysisSession.assertStableSymbolResult(testServices: TestServices, firstInfo: KtCallInfo?, secondInfo: KtCallInfo?) {
        val assertions = testServices.assertions
        if (firstInfo == null || secondInfo == null) {
            assertions.assertEquals(firstInfo, secondInfo)
            return
        }

        assertions.assertEquals(firstInfo::class, secondInfo::class)

        val firstCalls = sortedCalls(firstInfo.calls)
        val secondCalls = sortedCalls(secondInfo.calls)
        assertions.assertEquals(firstCalls.size, secondCalls.size)

        for ((firstCall, secondCall) in firstCalls.zip(secondCalls)) {
            assertions.assertEquals(firstCall::class, secondCall::class)
            val symbolsFromFirstCall = firstCall.symbols()
            val symbolsFromSecondCall = secondCall.symbols()
            assertions.assertEquals(symbolsFromFirstCall, symbolsFromSecondCall)
        }
    }
}
