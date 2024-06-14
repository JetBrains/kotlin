/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.contracts.description

import com.google.common.base.Objects
import org.checkerframework.checker.signature.qual.DotSeparatedIdentifiersOrPrimitiveType
import org.jetbrains.kotlin.analysis.api.contracts.description.booleans.KaContractBooleanExpression
import org.jetbrains.kotlin.analysis.api.lifetime.KaLifetimeOwner
import org.jetbrains.kotlin.analysis.api.lifetime.KaLifetimeToken
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.contracts.description.EventOccurrencesRange

/**
 * Represents [kotlin.contracts.Effect].
 */
public sealed interface KaContractEffectDeclaration : KaLifetimeOwner

@Deprecated("Use 'KaContractEffectDeclaration' instead", ReplaceWith("KaContractEffectDeclaration"))
public typealias KtContractEffectDeclaration = KaContractEffectDeclaration

/**
 * Represents [kotlin.contracts.ContractBuilder.callsInPlace].
 */
public class KaContractCallsInPlaceContractEffectDeclaration(
    private val backingValueParameterReference: KaContractParameterValue,
    private val backingOccurrencesRange: EventOccurrencesRange,
) : KaContractEffectDeclaration {
    override val token: KaLifetimeToken get() = backingValueParameterReference.token

    public val valueParameterReference: KaContractParameterValue get() = withValidityAssertion { backingValueParameterReference }
    public val occurrencesRange: EventOccurrencesRange get() = withValidityAssertion { backingOccurrencesRange }

    override fun hashCode(): Int = Objects.hashCode(backingValueParameterReference, backingOccurrencesRange)
    override fun equals(other: Any?): Boolean {
        return this === other ||
                other is KaContractCallsInPlaceContractEffectDeclaration &&
                other.backingValueParameterReference == backingValueParameterReference &&
                other.backingOccurrencesRange == backingOccurrencesRange
    }
}

@Deprecated(
    "Use 'KaContractCallsInPlaceContractEffectDeclaration' instead",
    ReplaceWith("KaContractCallsInPlaceContractEffectDeclaration")
)
public typealias KtContractCallsInPlaceContractEffectDeclaration = KaContractCallsInPlaceContractEffectDeclaration

/**
 * Represents [kotlin.contracts.SimpleEffect.implies].
 */
public class KaContractConditionalContractEffectDeclaration(
    private val backingEffect: KaContractEffectDeclaration,
    private val backingCondition: KaContractBooleanExpression
) : KaContractEffectDeclaration {
    override val token: KaLifetimeToken get() = backingEffect.token

    public val effect: KaContractEffectDeclaration get() = withValidityAssertion { backingEffect }
    public val condition: KaContractBooleanExpression get() = withValidityAssertion { backingCondition }

    override fun hashCode(): Int = Objects.hashCode(backingEffect, backingCondition)
    override fun equals(other: Any?): Boolean {
        return this === other ||
                other is KaContractConditionalContractEffectDeclaration &&
                other.backingEffect == backingEffect &&
                other.backingCondition == backingCondition
    }
}

@Deprecated(
    "Use 'KaContractConditionalContractEffectDeclaration' instead",
    ReplaceWith("KaContractConditionalContractEffectDeclaration")
)
public typealias KtContractConditionalContractEffectDeclaration = KaContractConditionalContractEffectDeclaration

/**
 * Represents [kotlin.contracts.ContractBuilder.returnsNotNull] & [kotlin.contracts.ContractBuilder.returns].
 */
public sealed class KaContractReturnsContractEffectDeclaration : KaContractEffectDeclaration {
    /**
     * Represent [kotlin.contracts.ContractBuilder.returnsNotNull].
     */
    public class KaContractReturnsNotNullEffectDeclaration(
        override val token: KaLifetimeToken
    ) : KaContractReturnsContractEffectDeclaration() {
        override fun equals(other: Any?): Boolean = other is KaContractReturnsNotNullEffectDeclaration
        override fun hashCode(): Int = javaClass.hashCode()
    }

    /**
     * Represents [kotlin.contracts.ContractBuilder.returns] with a `value` argument.
     */
    public class KaContractReturnsSpecificValueEffectDeclaration(
        private val backingValue: KaContractConstantValue
    ) : KaContractReturnsContractEffectDeclaration() {
        override val token: KaLifetimeToken get() = backingValue.token
        public val value: KaContractConstantValue get() = withValidityAssertion { backingValue }

        override fun equals(other: Any?): Boolean {
            return this === other ||
                    other is KaContractReturnsSpecificValueEffectDeclaration &&
                    other.backingValue == backingValue
        }

        override fun hashCode(): Int = backingValue.hashCode()
    }

    /**
     * Represents [kotlin.contracts.ContractBuilder.returns] without arguments.
     */
    public class KaContractReturnsSuccessfullyEffectDeclaration(
        override val token: KaLifetimeToken
    ) : KaContractReturnsContractEffectDeclaration() {
        override fun equals(other: Any?): Boolean = other is KaContractReturnsSuccessfullyEffectDeclaration
        override fun hashCode(): Int = javaClass.hashCode()
    }
}

@Deprecated("Use 'KaContractReturnsContractEffectDeclaration' instead", ReplaceWith("KaContractReturnsContractEffectDeclaration"))
public typealias KtContractReturnsContractEffectDeclaration = KaContractReturnsContractEffectDeclaration