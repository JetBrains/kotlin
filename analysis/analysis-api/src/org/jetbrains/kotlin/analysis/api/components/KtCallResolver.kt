/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.components

import org.jetbrains.kotlin.analysis.api.calls.KtCall
import org.jetbrains.kotlin.psi.*

public abstract class KtCallResolver : KtAnalysisSessionComponent() {
    public abstract fun resolveAccessorCall(call: KtSimpleNameExpression): KtCall?
    public abstract fun resolveCall(call: KtCallElement): KtCall?
    public abstract fun resolveCall(call: KtBinaryExpression): KtCall?
    public abstract fun resolveCall(call: KtUnaryExpression): KtCall?
    public abstract fun resolveCall(call: KtArrayAccessExpression): KtCall?
}

public interface KtCallResolverMixIn : KtAnalysisSessionMixIn {
    /**
     * Resolves the given simple name expression to an accessor call if that name refers to a property.
     *
     * This spans both Kotlin property and synthetic Java property.
     */
    public fun KtSimpleNameExpression.resolveAccessorCall(): KtCall? =
        analysisSession.callResolver.resolveAccessorCall(this)

    public fun KtCallElement.resolveCall(): KtCall? =
        analysisSession.callResolver.resolveCall(this)

    public fun KtBinaryExpression.resolveCall(): KtCall? =
        analysisSession.callResolver.resolveCall(this)

    public fun KtUnaryExpression.resolveCall(): KtCall? =
        analysisSession.callResolver.resolveCall(this)

    public fun KtArrayAccessExpression.resolveCall(): KtCall? =
        analysisSession.callResolver.resolveCall(this)

    public fun KtElement.resolveCallIfPossible(): KtCall? = when (this) {
        is KtCallElement -> resolveCall()
        is KtBinaryExpression -> resolveCall()
        is KtUnaryExpression -> resolveCall()
        is KtArrayAccessExpression -> resolveCall()
        else -> null
    }
}