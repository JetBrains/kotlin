/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.components

import org.jetbrains.kotlin.analysis.api.ValidityTokenOwner
import org.jetbrains.kotlin.analysis.api.tokens.ValidityToken
import org.jetbrains.kotlin.analysis.api.types.KtType
import org.jetbrains.kotlin.analysis.api.withValidityAssertion
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
        analysisSession.smartCastProvider.getSmartCastedInfo(this)

    public fun KtExpression.getImplicitReceiverSmartCast(): Collection<KtImplicitReceiverSmartCast> =
        analysisSession.smartCastProvider.getImplicitReceiverSmartCast(this)
}

public data class KtSmartCastInfo(
    private val _smartCastType: KtType,
    private val _isStable: Boolean,
    override val token: ValidityToken
) : ValidityTokenOwner {
    public val isStable: Boolean get() = withValidityAssertion { _isStable }
    public val smartCastType: KtType get() = withValidityAssertion { _smartCastType }
}

public data class KtImplicitReceiverSmartCast(
    private val _type: KtType,
    private val _kind: KtImplicitReceiverSmartCastKind,
    override val token: ValidityToken
) : ValidityTokenOwner {
    public val type: KtType get() = withValidityAssertion { _type }
    public val kind: KtImplicitReceiverSmartCastKind get() = withValidityAssertion { _kind }
}

public enum class KtImplicitReceiverSmartCastKind {
    DISPATCH, EXTENSION
}
