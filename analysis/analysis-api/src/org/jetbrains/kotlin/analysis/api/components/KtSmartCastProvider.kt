/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.components

import org.jetbrains.kotlin.analysis.api.lifetime.KtLifetimeOwner
import org.jetbrains.kotlin.analysis.api.lifetime.KtLifetimeToken
import org.jetbrains.kotlin.analysis.api.types.KtType
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.psi.KtExpression

public abstract class KtSmartCastProvider : KtAnalysisSessionComponent() {
    public abstract fun getSmartCastedInfo(expression: KtExpression): KtSmartCastInfo?
    public abstract fun getImplicitReceiverSmartCast(expression: KtExpression): Collection<KtImplicitReceiverSmartCast>
}

public interface KtSmartCastProviderMixIn : KtAnalysisSessionMixIn {
    /**
     * Gets the smart-cast information of the given expression or null if the expression is not smart casted.
     */
    public fun KtExpression.getSmartCastInfo(): KtSmartCastInfo? =
        withValidityAssertion { analysisSession.smartCastProvider.getSmartCastedInfo(this) }

    /**
     * Returns the list of implicit smart-casts which are required for the expression to be called. Includes only implicit
     * smart-casts:
     *
     * ```kt
     * if (this is String) {
     *   this.substring() // 'this' receiver is explicit, so no implicit smart-cast here.
     *
     *   smartcast() // 'this' receiver is implicit, therefore there is implicit smart-cast involved.
     * }
     * ```
     */
    public fun KtExpression.getImplicitReceiverSmartCast(): Collection<KtImplicitReceiverSmartCast> =
        withValidityAssertion { analysisSession.smartCastProvider.getImplicitReceiverSmartCast(this) }
}

public data class KtSmartCastInfo(
    private val _smartCastType: KtType,
    private val _isStable: Boolean,
    override val token: KtLifetimeToken
) : KtLifetimeOwner {
    public val isStable: Boolean get() = withValidityAssertion { _isStable }
    public val smartCastType: KtType get() = withValidityAssertion { _smartCastType }
}

public data class KtImplicitReceiverSmartCast(
    private val _type: KtType,
    private val _kind: KtImplicitReceiverSmartCastKind,
    override val token: KtLifetimeToken
) : KtLifetimeOwner {
    public val type: KtType get() = withValidityAssertion { _type }
    public val kind: KtImplicitReceiverSmartCastKind get() = withValidityAssertion { _kind }
}

public enum class KtImplicitReceiverSmartCastKind {
    DISPATCH, EXTENSION
}
