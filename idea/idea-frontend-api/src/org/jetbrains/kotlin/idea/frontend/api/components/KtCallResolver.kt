/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.frontend.api.components

import org.jetbrains.kotlin.idea.frontend.api.calls.KtCall
import org.jetbrains.kotlin.psi.KtArrayAccessExpression
import org.jetbrains.kotlin.psi.KtBinaryExpression
import org.jetbrains.kotlin.psi.KtCallElement
import org.jetbrains.kotlin.psi.KtUnaryExpression

public abstract class KtCallResolver : KtAnalysisSessionComponent() {
    public abstract fun resolveCall(call: KtCallElement): KtCall?
    public abstract fun resolveCall(call: KtBinaryExpression): KtCall?
    public abstract fun resolveCall(call: KtUnaryExpression): KtCall?
    public abstract fun resolveCall(call: KtArrayAccessExpression): KtCall?
}

public interface KtCallResolverMixIn : KtAnalysisSessionMixIn {
    public fun KtCallElement.resolveCall(): KtCall? =
        analysisSession.callResolver.resolveCall(this)

    public fun KtBinaryExpression.resolveCall(): KtCall? =
        analysisSession.callResolver.resolveCall(this)

    public fun KtUnaryExpression.resolveCall(): KtCall? =
        analysisSession.callResolver.resolveCall(this)

    public fun KtArrayAccessExpression.resolveCall(): KtCall? =
        analysisSession.callResolver.resolveCall(this)
}
