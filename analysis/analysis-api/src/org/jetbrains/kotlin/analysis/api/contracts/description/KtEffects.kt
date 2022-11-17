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
 * K1: [org.jetbrains.kotlin.contracts.description.CallsEffectDeclaration]
 * K2: [org.jetbrains.kotlin.fir.contracts.description.ConeCallsEffectDeclaration]
 */
public class KtCallsEffectDeclaration(
    private val _valueParameterReference: KtAbstractValueParameterReference,
    private val _kind: EventOccurrencesRange,
) : KtEffectDeclaration {
    override val token: KtLifetimeToken get() = _valueParameterReference.token

    public val valueParameterReference: KtAbstractValueParameterReference get() = withValidityAssertion { _valueParameterReference }
    public val kind: EventOccurrencesRange get() = withValidityAssertion { _kind }
}

/**
 * K1: [org.jetbrains.kotlin.contracts.description.ConditionalEffectDeclaration]
 * K2: [org.jetbrains.kotlin.fir.contracts.description.ConeConditionalEffectDeclaration]
 */
public class KtConditionalEffectDeclaration(
    private val _effect: KtEffectDeclaration,
    private val _condition: KtBooleanExpression
) : KtEffectDeclaration {
    override val token: KtLifetimeToken get() = _effect.token

    public val effect: KtEffectDeclaration get() = withValidityAssertion { _effect }
    public val condition: KtBooleanExpression get() = withValidityAssertion { _condition }
}

/**
 * K1: [org.jetbrains.kotlin.contracts.description.ReturnsEffectDeclaration]
 * K2: [org.jetbrains.kotlin.fir.contracts.description.ConeReturnsEffectDeclaration]
 */
public class KtReturnsEffectDeclaration(private val _value: KtContractDescriptionValue) : KtEffectDeclaration {
    override val token: KtLifetimeToken get() = _value.token

    public val value: KtContractDescriptionValue get() = withValidityAssertion { _value }
}
