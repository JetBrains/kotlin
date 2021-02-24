/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.frontend.api.components

import org.jetbrains.kotlin.idea.frontend.api.calls.KtCall
import org.jetbrains.kotlin.psi.KtBinaryExpression
import org.jetbrains.kotlin.psi.KtCallExpression

abstract class KtCallResolver : KtAnalysisSessionComponent() {
    abstract fun resolveCall(call: KtCallExpression): KtCall?
    abstract fun resolveCall(call: KtBinaryExpression): KtCall?
}

interface KtCallResolverMixIn : KtAnalysisSessionMixIn {
    fun KtCallExpression.resolveCall(): KtCall? =
        analysisSession.callResolver.resolveCall(this)

    fun KtBinaryExpression.resolveCall(): KtCall? =
        analysisSession.callResolver.resolveCall(this)

}