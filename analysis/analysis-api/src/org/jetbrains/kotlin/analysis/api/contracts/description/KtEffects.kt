/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.contracts.description

import com.google.common.base.Objects
import org.jetbrains.kotlin.analysis.api.contracts.description.booleans.KtContractBooleanExpression
import org.jetbrains.kotlin.analysis.api.lifetime.KtLifetimeOwner
import org.jetbrains.kotlin.analysis.api.lifetime.KtLifetimeToken
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.contracts.description.EventOccurrencesRange

/**
 * Represents [kotlin.contracts.Effect].
 */
public sealed interface KtContractEffectDeclaration : KtLifetimeOwner

/**
 * Represents [kotlin.contracts.ContractBuilder.callsInPlace].
 */
public class KtContractCallsInPlaceContractEffectDeclaration(
    private val backingValueParameterReference: KtContractParameterValue,
    private val backingOccurrencesRange: EventOccurrencesRange,
) : KtContractEffectDeclaration {
    override val token: KtLifetimeToken get() = backingValueParameterReference.token

    public val valueParameterReference: KtContractParameterValue get() = withValidityAssertion { backingValueParameterReference }
    public val occurrencesRange: EventOccurrencesRange get() = withValidityAssertion { backingOccurrencesRange }

    override fun hashCode(): Int = Objects.hashCode(backingValueParameterReference, backingOccurrencesRange)
    override fun equals(other: Any?): Boolean {
        return this === other ||
                other is KtContractCallsInPlaceContractEffectDeclaration &&
                other.backingValueParameterReference == backingValueParameterReference &&
                other.backingOccurrencesRange == backingOccurrencesRange
    }
}

/**
 * Represents [kotlin.contracts.SimpleEffect.implies].
 */
public class KtContractConditionalContractEffectDeclaration(
    private val backingEffect: KtContractEffectDeclaration,
    private val backingCondition: KtContractBooleanExpression
) : KtContractEffectDeclaration {
    override val token: KtLifetimeToken get() = backingEffect.token

    public val effect: KtContractEffectDeclaration get() = withValidityAssertion { backingEffect }
    public val condition: KtContractBooleanExpression get() = withValidityAssertion { backingCondition }

    override fun hashCode(): Int = Objects.hashCode(backingEffect, backingCondition)
    override fun equals(other: Any?): Boolean {
        return this === other ||
                other is KtContractConditionalContractEffectDeclaration &&
                other.backingEffect == backingEffect &&
                other.backingCondition == backingCondition
    }
}

/**
 * Represents [kotlin.contracts.ContractBuilder.returnsNotNull] & [kotlin.contracts.ContractBuilder.returns].
 */
public sealed class KtContractReturnsContractEffectDeclaration : KtContractEffectDeclaration {
    /**
     * Represent [kotlin.contracts.ContractBuilder.returnsNotNull].
     */
    public class KtContractReturnsNotNullEffectDeclaration(
        override val token: KtLifetimeToken
    ) : KtContractReturnsContractEffectDeclaration() {
        override fun equals(other: Any?): Boolean = other is KtContractReturnsNotNullEffectDeclaration
        override fun hashCode(): Int = javaClass.hashCode()
    }

    /**
     * Represents [kotlin.contracts.ContractBuilder.returns] with a `value` argument.
     */
    public class KtContractReturnsSpecificValueEffectDeclaration(
        private val backingValue: KtContractConstantValue
    ) : KtContractReturnsContractEffectDeclaration() {
        override val token: KtLifetimeToken get() = backingValue.token
        public val value: KtContractConstantValue get() = withValidityAssertion { backingValue }

        override fun equals(other: Any?): Boolean {
            return this === other ||
                    other is KtContractReturnsSpecificValueEffectDeclaration &&
                    other.backingValue == backingValue
        }

        override fun hashCode(): Int = backingValue.hashCode()
    }

    /**
     * Represents [kotlin.contracts.ContractBuilder.returns] without arguments.
     */
    public class KtContractReturnsSuccessfullyEffectDeclaration(
        override val token: KtLifetimeToken
    ) : KtContractReturnsContractEffectDeclaration() {
        override fun equals(other: Any?): Boolean = other is KtContractReturnsSuccessfullyEffectDeclaration
        override fun hashCode(): Int = javaClass.hashCode()
    }
}
