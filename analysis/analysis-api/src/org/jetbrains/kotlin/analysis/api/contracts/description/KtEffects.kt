/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
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
    private val _valueParameterReference: KtContractParameterValue,
    private val _occurrencesRange: EventOccurrencesRange,
) : KtContractEffectDeclaration {
    override val token: KtLifetimeToken get() = _valueParameterReference.token

    public val valueParameterReference: KtContractParameterValue get() = withValidityAssertion { _valueParameterReference }
    public val occurrencesRange: EventOccurrencesRange get() = withValidityAssertion { _occurrencesRange }

    override fun hashCode(): Int = Objects.hashCode(_valueParameterReference, _occurrencesRange)
    override fun equals(other: Any?): Boolean =
        other is KtContractCallsInPlaceContractEffectDeclaration && other._valueParameterReference == _valueParameterReference &&
                other._occurrencesRange == _occurrencesRange
}

/**
 * Represents [kotlin.contracts.SimpleEffect.implies].
 */
public class KtContractConditionalContractEffectDeclaration(
    private val _effect: KtContractEffectDeclaration,
    private val _condition: KtContractBooleanExpression
) : KtContractEffectDeclaration {
    override val token: KtLifetimeToken get() = _effect.token

    public val effect: KtContractEffectDeclaration get() = withValidityAssertion { _effect }
    public val condition: KtContractBooleanExpression get() = withValidityAssertion { _condition }

    override fun hashCode(): Int = Objects.hashCode(_effect, _condition)
    override fun equals(other: Any?): Boolean =
        other is KtContractConditionalContractEffectDeclaration && other._effect == _effect && other._condition == _condition
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
        private val _value: KtContractConstantValue
    ) : KtContractReturnsContractEffectDeclaration() {
        override val token: KtLifetimeToken get() = _value.token
        public val value: KtContractConstantValue get() = withValidityAssertion { _value }

        override fun equals(other: Any?): Boolean = other is KtContractReturnsSpecificValueEffectDeclaration && other._value == _value
        override fun hashCode(): Int = _value.hashCode()
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
