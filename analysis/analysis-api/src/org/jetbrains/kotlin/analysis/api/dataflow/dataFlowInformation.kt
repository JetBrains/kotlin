/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.dataflow

import org.jetbrains.kotlin.analysis.api.KaImplementationDetail
import org.jetbrains.kotlin.analysis.api.KaNonPublicApi
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.internals.internals
import org.jetbrains.kotlin.psi.KtExpression

/**
 * [Smart cast information][KaSmartCastInfo] for the given [KtExpression], or `null` if smart casts are not applied to it.
 */
context(session: KaSession)
public val KtExpression.smartCastInfo: KaSmartCastInfo?
    get() {
        @OptIn(KaImplementationDetail::class)
        return internals.dataFlowProvider.smartCastInfo(this)
    }

/**
 * The list of [implicit receiver smart casts][KaImplicitReceiverSmartCast] which have refined the expression's implicit receivers to a
 * more specific type. These smart casts are required for the expression to be evaluated. The list does not include smart casts for
 * explicit receivers.
 *
 * #### Example
 *
 * ```kotlin
 * if (this is String) {
 *   this.substring()   // 'this' receiver is explicit, so there is no implicit smart cast here.
 *
 *   smartcast()        // 'this' receiver is implicit, therefore there is an implicit smart cast involved.
 * }
 * ```
 */
@KaNonPublicApi
context(session: KaSession)
public val KtExpression.implicitReceiverSmartCasts: Collection<KaImplicitReceiverSmartCast>
    get() {
        @OptIn(KaImplementationDetail::class)
        return internals.dataFlowProvider.implicitReceiverSmartCasts(this)
    }

@KaNonPublicApi
context(session: KaSession)
public fun computeExitPointSnapshot(statements: List<KtExpression>): KaDataFlowExitPointSnapshot {
    @OptIn(KaImplementationDetail::class)
    return internals.dataFlowProvider.computeExitPointSnapshot(statements)
}
