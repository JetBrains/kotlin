/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.internals

import org.jetbrains.kotlin.analysis.api.KaImplementationDetail
import org.jetbrains.kotlin.analysis.api.KaNonPublicApi
import org.jetbrains.kotlin.analysis.api.dataflow.KaDataFlowExitPointSnapshot
import org.jetbrains.kotlin.analysis.api.dataflow.KaImplicitReceiverSmartCast
import org.jetbrains.kotlin.analysis.api.dataflow.KaSmartCastInfo
import org.jetbrains.kotlin.psi.KtExpression

@KaImplementationDetail
@SubclassOptInRequired(KaImplementationDetail::class)
public interface KaInternalsDataFlowProvider {
    public fun smartCastInfo(expression: KtExpression): KaSmartCastInfo?

    @KaNonPublicApi
    public fun implicitReceiverSmartCasts(expression: KtExpression): Collection<KaImplicitReceiverSmartCast>

    @KaNonPublicApi
    public fun computeExitPointSnapshot(statements: List<KtExpression>): KaDataFlowExitPointSnapshot
}
