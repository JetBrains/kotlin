/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.components

import org.jetbrains.kotlin.analysis.api.ImplicitReceiverSmartCast
import org.jetbrains.kotlin.analysis.api.types.KtType
import org.jetbrains.kotlin.psi.KtExpression

public abstract class KtSmartCastProvider : KtAnalysisSessionComponent() {
    public abstract fun getSmartCastedInfo(expression: KtExpression): SmartCastInfo?
    public abstract fun getImplicitReceiverSmartCast(expression: KtExpression): Collection<ImplicitReceiverSmartCast>
}

public class SmartCastInfo(public val smartCastType: KtType, public val isStable: Boolean)

public interface KtSmartCastProviderMixIn : KtAnalysisSessionMixIn {
    /**
     * Gets the smart-cast information of the given expression or null if the expression is not smart casted.
     */
    public fun KtExpression.getSmartCastInfo(): SmartCastInfo? =
        analysisSession.smartCastProvider.getSmartCastedInfo(this)

    public fun KtExpression.getImplicitReceiverSmartCast(): Collection<ImplicitReceiverSmartCast> =
        analysisSession.smartCastProvider.getImplicitReceiverSmartCast(this)
}