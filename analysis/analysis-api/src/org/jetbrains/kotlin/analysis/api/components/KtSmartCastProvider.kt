/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.components

import org.jetbrains.kotlin.analysis.api.lifetime.KaLifetimeOwner
import org.jetbrains.kotlin.analysis.api.lifetime.KaLifetimeToken
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.types.KaType
import org.jetbrains.kotlin.psi.KtExpression
import java.util.Objects

public abstract class KaSmartCastProvider : KaSessionComponent() {
    public abstract fun getSmartCastedInfo(expression: KtExpression): KaSmartCastInfo?
    public abstract fun getImplicitReceiverSmartCast(expression: KtExpression): Collection<KaImplicitReceiverSmartCast>
}

public typealias KtSmartCastProvider = KaSmartCastProvider

public interface KaSmartCastProviderMixIn : KaSessionMixIn {
    /**
     * Gets the smart-cast information of the given expression or null if the expression is not smart casted.
     */
    public fun KtExpression.getSmartCastInfo(): KaSmartCastInfo? =
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
    public fun KtExpression.getImplicitReceiverSmartCast(): Collection<KaImplicitReceiverSmartCast> =
        withValidityAssertion { analysisSession.smartCastProvider.getImplicitReceiverSmartCast(this) }
}

public typealias KtSmartCastProviderMixIn = KaSmartCastProviderMixIn

public class KaSmartCastInfo(
    private val backingSmartCastType: KaType,
    private val backingIsStable: Boolean,
    override val token: KaLifetimeToken
) : KaLifetimeOwner {
    public val isStable: Boolean get() = withValidityAssertion { backingIsStable }
    public val smartCastType: KaType get() = withValidityAssertion { backingSmartCastType }

    override fun equals(other: Any?): Boolean {
        return this === other ||
                other is KaSmartCastInfo &&
                other.backingSmartCastType == backingSmartCastType &&
                other.backingIsStable == backingIsStable
    }

    override fun hashCode(): Int = Objects.hash(backingSmartCastType, backingIsStable)
}

public typealias KtSmartCastInfo = KaSmartCastInfo

public class KaImplicitReceiverSmartCast(
    private val backingType: KaType,
    private val backingKind: KaImplicitReceiverSmartCastKind,
    override val token: KaLifetimeToken
) : KaLifetimeOwner {
    public val type: KaType get() = withValidityAssertion { backingType }
    public val kind: KaImplicitReceiverSmartCastKind get() = withValidityAssertion { backingKind }

    override fun equals(other: Any?): Boolean {
        return this === other ||
                other is KaImplicitReceiverSmartCast &&
                other.backingType == backingType &&
                other.backingKind == backingKind
    }

    override fun hashCode(): Int {
        return Objects.hash(backingType, backingKind)
    }
}

public typealias KtImplicitReceiverSmartCast = KaImplicitReceiverSmartCast

public enum class KaImplicitReceiverSmartCastKind {
    DISPATCH, EXTENSION
}

public typealias KtImplicitReceiverSmartCastKind = KaImplicitReceiverSmartCastKind