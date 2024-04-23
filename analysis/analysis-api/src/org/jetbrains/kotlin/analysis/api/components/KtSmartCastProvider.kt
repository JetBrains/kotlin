/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.components

import org.jetbrains.kotlin.analysis.api.lifetime.KtLifetimeOwner
import org.jetbrains.kotlin.analysis.api.lifetime.KtLifetimeToken
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.types.KtType
import org.jetbrains.kotlin.psi.KtExpression
import java.util.Objects

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

public class KtSmartCastInfo(
    private val backingSmartCastType: KtType,
    private val backingIsStable: Boolean,
    override val token: KtLifetimeToken
) : KtLifetimeOwner {
    public val isStable: Boolean get() = withValidityAssertion { backingIsStable }
    public val smartCastType: KtType get() = withValidityAssertion { backingSmartCastType }

    override fun equals(other: Any?): Boolean {
        return this === other ||
                other is KtSmartCastInfo &&
                other.backingSmartCastType == backingSmartCastType &&
                other.backingIsStable == backingIsStable
    }

    override fun hashCode(): Int = Objects.hash(backingSmartCastType, backingIsStable)
}

public class KtImplicitReceiverSmartCast(
    private val backingType: KtType,
    private val backingKind: KtImplicitReceiverSmartCastKind,
    override val token: KtLifetimeToken
) : KtLifetimeOwner {
    public val type: KtType get() = withValidityAssertion { backingType }
    public val kind: KtImplicitReceiverSmartCastKind get() = withValidityAssertion { backingKind }

    override fun equals(other: Any?): Boolean {
        return this === other ||
                other is KtImplicitReceiverSmartCast &&
                other.backingType == backingType &&
                other.backingKind == backingKind
    }

    override fun hashCode(): Int {
        return Objects.hash(backingType, backingKind)
    }
}

public enum class KtImplicitReceiverSmartCastKind {
    DISPATCH, EXTENSION
}
