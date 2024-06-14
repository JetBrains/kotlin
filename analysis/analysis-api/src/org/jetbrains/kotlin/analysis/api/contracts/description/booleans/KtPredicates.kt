/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.contracts.description.booleans

import com.google.common.base.Objects
import org.jetbrains.kotlin.analysis.api.contracts.description.KaContractParameterValue
import org.jetbrains.kotlin.analysis.api.lifetime.KaLifetimeToken
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.types.KaType

/**
 * See: [KaContractBooleanExpression].
 */
public class KaContractIsInstancePredicateExpression(
    private val backingArgument: KaContractParameterValue,
    private val backingType: KaType,
    private val backingIsNegated: Boolean
) : KaContractBooleanExpression {
    override val token: KaLifetimeToken get() = backingType.token
    public val argument: KaContractParameterValue get() = withValidityAssertion { backingArgument }
    public val type: KaType get() = withValidityAssertion { backingType }
    public val isNegated: Boolean get() = withValidityAssertion { backingIsNegated }
    public fun negated(): KaContractIsInstancePredicateExpression = KaContractIsInstancePredicateExpression(argument, type, !isNegated)

    override fun hashCode(): Int = Objects.hashCode(backingArgument, backingType, backingIsNegated)
    override fun equals(other: Any?): Boolean {
        return this === other ||
                other is KaContractIsInstancePredicateExpression &&
                other.backingArgument == backingArgument &&
                other.backingType == backingType &&
                other.backingIsNegated == backingIsNegated
    }
}

@Deprecated("Use 'KaContractIsInstancePredicateExpression' instead.", ReplaceWith("KaContractIsInstancePredicateExpression"))
public typealias KtContractIsInstancePredicateExpression = KaContractIsInstancePredicateExpression

/**
 * See: [KaContractBooleanExpression].
 */
public class KaContractIsNullPredicateExpression(
    private val backingArgument: KaContractParameterValue,
    private val backingIsNegated: Boolean
) : KaContractBooleanExpression {
    override val token: KaLifetimeToken get() = backingArgument.token
    public val argument: KaContractParameterValue get() = withValidityAssertion { backingArgument }
    public val isNegated: Boolean get() = withValidityAssertion { backingIsNegated }
    public fun negated(): KaContractIsNullPredicateExpression = KaContractIsNullPredicateExpression(argument, !isNegated)

    override fun hashCode(): Int = Objects.hashCode(backingArgument, backingIsNegated)
    override fun equals(other: Any?): Boolean {
        return this === other ||
                other is KaContractIsNullPredicateExpression &&
                other.backingArgument == backingArgument &&
                other.backingIsNegated == backingIsNegated
    }
}

@Deprecated("Use 'KaContractIsNullPredicateExpression' instead.", ReplaceWith("KaContractIsNullPredicateExpression"))
public typealias KtContractIsNullPredicateExpression = KaContractIsNullPredicateExpression