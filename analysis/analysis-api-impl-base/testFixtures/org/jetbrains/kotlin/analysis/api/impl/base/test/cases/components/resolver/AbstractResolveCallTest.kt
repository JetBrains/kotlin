/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.resolver

import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.components.resolveToCall
import org.jetbrains.kotlin.analysis.api.components.tryResolveCall
import org.jetbrains.kotlin.analysis.api.impl.base.resolution.KaBaseErrorCallInfo
import org.jetbrains.kotlin.analysis.api.impl.base.resolution.KaBaseSuccessCallInfo
import org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.assertStableResult
import org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.stringRepresentation
import org.jetbrains.kotlin.analysis.api.resolution.KaCall
import org.jetbrains.kotlin.analysis.api.resolution.KaCallInfo
import org.jetbrains.kotlin.analysis.api.resolution.KaCallResolutionError
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtExperimentalApi
import org.jetbrains.kotlin.resolution.KtResolvableCall
import org.jetbrains.kotlin.test.services.TestServices

abstract class AbstractResolveCallTest : AbstractResolveByElementTest() {
    override val resolveKind: String get() = "call"

    override fun generateResolveOutput(mainElement: KtElement, testServices: TestServices): String = analyzeForTest(mainElement) {
        val call = tryResolveCall(mainElement)
        val secondCall = tryResolveCall(mainElement)

        ignoreStabilityIfNeeded {
            assertStableResult(testServices, call?.asCallInfo(), secondCall?.asCallInfo())
        }

        stringRepresentation(call)
    }

    private fun Any.asCallInfo(): KaCallInfo? = when (this) {
        is KaCallInfo -> this
        is KaCall -> KaBaseSuccessCallInfo(this)
        is KaCallResolutionError -> KaBaseErrorCallInfo(candidateCalls, diagnostic)
        else -> error("Unknown type: ${this::class.simpleName}")
    }

    @OptIn(KtExperimentalApi::class)
    context(_: KaSession)
    private fun tryResolveCall(element: KtElement): Any? = if (element is KtResolvableCall) {
        element.tryResolveCall()
    } else {
        element.resolveToCall()
    }
}
