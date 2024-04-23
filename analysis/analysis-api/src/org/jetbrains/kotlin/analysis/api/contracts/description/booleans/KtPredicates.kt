/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.contracts.description.booleans

import com.google.common.base.Objects
import org.jetbrains.kotlin.analysis.api.contracts.description.KtContractParameterValue
import org.jetbrains.kotlin.analysis.api.lifetime.KtLifetimeToken
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.types.KtType

/**
 * See: [KtContractBooleanExpression].
 */
public class KtContractIsInstancePredicateExpression(
    private val backingArgument: KtContractParameterValue,
    private val backingType: KtType,
    private val backingIsNegated: Boolean
) : KtContractBooleanExpression {
    override val token: KtLifetimeToken get() = backingType.token
    public val argument: KtContractParameterValue get() = withValidityAssertion { backingArgument }
    public val type: KtType get() = withValidityAssertion { backingType }
    public val isNegated: Boolean get() = withValidityAssertion { backingIsNegated }
    public fun negated(): KtContractIsInstancePredicateExpression = KtContractIsInstancePredicateExpression(argument, type, !isNegated)

    override fun hashCode(): Int = Objects.hashCode(backingArgument, backingType, backingIsNegated)
    override fun equals(other: Any?): Boolean {
        return this === other ||
                other is KtContractIsInstancePredicateExpression &&
                other.backingArgument == backingArgument &&
                other.backingType == backingType &&
                other.backingIsNegated == backingIsNegated
    }
}

/**
 * See: [KtContractBooleanExpression].
 */
public class KtContractIsNullPredicateExpression(
    private val backingArgument: KtContractParameterValue,
    private val backingIsNegated: Boolean
) : KtContractBooleanExpression {
    override val token: KtLifetimeToken get() = backingArgument.token
    public val argument: KtContractParameterValue get() = withValidityAssertion { backingArgument }
    public val isNegated: Boolean get() = withValidityAssertion { backingIsNegated }
    public fun negated(): KtContractIsNullPredicateExpression = KtContractIsNullPredicateExpression(argument, !isNegated)

    override fun hashCode(): Int = Objects.hashCode(backingArgument, backingIsNegated)
    override fun equals(other: Any?): Boolean {
        return this === other ||
                other is KtContractIsNullPredicateExpression &&
                other.backingArgument == backingArgument &&
                other.backingIsNegated == backingIsNegated
    }
}
