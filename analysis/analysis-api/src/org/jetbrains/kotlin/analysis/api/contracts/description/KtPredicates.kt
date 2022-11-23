/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.contracts.description

import org.jetbrains.kotlin.analysis.api.lifetime.KtLifetimeToken
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.types.KtType

/**
 * K1: [org.jetbrains.kotlin.contracts.description.expressions.IsInstancePredicate]
 * K2: [org.jetbrains.kotlin.fir.contracts.description.ConeIsInstancePredicate]
 */
public class KtContractIsInstancePredicate(
    private val _argument: KtContractAbstractValueParameterReference,
    private val _type: KtType,
    private val _isNegated: Boolean
) : KtContractBooleanExpression {
    override val token: KtLifetimeToken get() = _type.token

    public val argument: KtContractAbstractValueParameterReference get() = withValidityAssertion { _argument }
    public val type: KtType get() = withValidityAssertion { _type }
    public val isNegated: Boolean get() = withValidityAssertion { _isNegated }

    public fun negated(): KtContractIsInstancePredicate = KtContractIsInstancePredicate(argument, type, !isNegated)
}

/**
 * K1: [org.jetbrains.kotlin.contracts.description.expressions.IsNullPredicate]
 * K2: [org.jetbrains.kotlin.fir.contracts.description.ConeIsNullPredicate]
 */
public class KtContractIsNullPredicate(
    private val _argument: KtContractAbstractValueParameterReference,
    private val _isNegated: Boolean
) : KtContractBooleanExpression {
    override val token: KtLifetimeToken get() = _argument.token
    public val argument: KtContractAbstractValueParameterReference get() = withValidityAssertion { _argument }
    public val isNegated: Boolean get() = withValidityAssertion { _isNegated }

    public fun negated(): KtContractIsNullPredicate = KtContractIsNullPredicate(argument, !isNegated)
}
