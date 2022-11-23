/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.contracts.description

import org.jetbrains.kotlin.analysis.api.lifetime.KtLifetimeToken
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.contracts.description.EventOccurrencesRange

/**
 * [kotlin.contracts.ContractBuilder.callsInPlace]
 *
 * K1: [org.jetbrains.kotlin.contracts.description.CallsEffectDeclaration]
 * K2: [org.jetbrains.kotlin.fir.contracts.description.ConeCallsEffectDeclaration]
 */
public class KtContractCallsContractEffectDeclaration(
    private val _valueParameterReference: KtContractAbstractValueParameterReference,
    private val _kind: EventOccurrencesRange,
) : KtContractEffectDeclaration {
    override val token: KtLifetimeToken get() = _valueParameterReference.token

    public val valueParameterReference: KtContractAbstractValueParameterReference get() = withValidityAssertion { _valueParameterReference }
    public val kind: EventOccurrencesRange get() = withValidityAssertion { _kind }
}

/**
 * [kotlin.contracts.SimpleEffect.implies]
 *
 * K1: [org.jetbrains.kotlin.contracts.description.ConditionalEffectDeclaration]
 * K2: [org.jetbrains.kotlin.fir.contracts.description.ConeConditionalEffectDeclaration]
 */
public class KtContractConditionalContractEffectDeclaration(
    private val _effect: KtContractEffectDeclaration,
    private val _condition: KtContractBooleanExpression
) : KtContractEffectDeclaration {
    override val token: KtLifetimeToken get() = _effect.token

    public val effect: KtContractEffectDeclaration get() = withValidityAssertion { _effect }
    public val condition: KtContractBooleanExpression get() = withValidityAssertion { _condition }
}

/**
 * [kotlin.contracts.ContractBuilder.returnsNotNull] & [kotlin.contracts.ContractBuilder.returns]
 *
 * K1: [org.jetbrains.kotlin.contracts.description.ReturnsEffectDeclaration]
 * K2: [org.jetbrains.kotlin.fir.contracts.description.ConeReturnsEffectDeclaration]
 */
public class KtContractReturnsContractEffectDeclaration(private val _value: KtContractDescriptionValue) : KtContractEffectDeclaration {
    override val token: KtLifetimeToken get() = _value.token

    public val value: KtContractDescriptionValue get() = withValidityAssertion { _value }
}
