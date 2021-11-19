/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.components

import org.jetbrains.kotlin.analysis.api.calls.KtCallInfo
import org.jetbrains.kotlin.psi.*

public abstract class KtCallResolver : KtAnalysisSessionComponent() {
    public abstract fun resolveCall(call: KtElement): KtCallInfo?
}

public interface KtCallResolverMixIn : KtAnalysisSessionMixIn {

    public fun KtElement.resolveCall(): KtCallInfo? =
        analysisSession.callResolver.resolveCall(this)

    public fun KtCallElement.resolveCall(): KtCallInfo =
        analysisSession.callResolver.resolveCall(this) ?: error("KtCallElement should always resolve to a KtCallInfo")

    public fun KtBinaryExpression.resolveCall(): KtCallInfo =
        analysisSession.callResolver.resolveCall(this) ?: error("KtBinaryExpression should always resolve to a KtCallInfo")

    public fun KtUnaryExpression.resolveCall(): KtCallInfo =
        analysisSession.callResolver.resolveCall(this) ?: error("KtUnaryExpression should always resolve to a KtCallInfo")

    public fun KtArrayAccessExpression.resolveCall(): KtCallInfo =
        analysisSession.callResolver.resolveCall(this) ?: error("KtArrayAccessExpression should always resolve to a KtCallInfo")
}