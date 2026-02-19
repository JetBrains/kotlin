/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.contracts.description.booleans

import com.google.common.base.Objects
import org.jetbrains.kotlin.analysis.api.KaImplementationDetail
import org.jetbrains.kotlin.analysis.api.contracts.description.KaContractParameterValue
import org.jetbrains.kotlin.analysis.api.contracts.description.booleans.KaContractIsInstancePredicateExpression
import org.jetbrains.kotlin.analysis.api.contracts.description.booleans.KaContractIsNullPredicateExpression
import org.jetbrains.kotlin.analysis.api.lifetime.KaLifetimeToken
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.types.KaType

@KaImplementationDetail
class KaBaseContractIsInstancePredicateExpression(
    private val backingArgument: KaContractParameterValue,
    private val backingType: KaType,
    private val backingIsNegated: Boolean
) : KaContractIsInstancePredicateExpression {
    override val token: KaLifetimeToken get() = backingType.token

    override val argument: KaContractParameterValue get() = withValidityAssertion { backingArgument }

    override val type: KaType get() = withValidityAssertion { backingType }

    override val isNegated: Boolean get() = withValidityAssertion { backingIsNegated }

    override fun negated(): KaContractIsInstancePredicateExpression =
        KaBaseContractIsInstancePredicateExpression(argument, type, !isNegated)

    override fun equals(other: Any?): Boolean {
        return this === other ||
                other is KaBaseContractIsInstancePredicateExpression &&
                other.backingArgument == backingArgument &&
                other.backingType == backingType &&
                other.backingIsNegated == backingIsNegated
    }

    override fun hashCode(): Int = Objects.hashCode(backingArgument, backingType, backingIsNegated)
}

@KaImplementationDetail
class KaBaseContractIsNullPredicateExpression(
    private val backingArgument: KaContractParameterValue,
    private val backingIsNegated: Boolean
) : KaContractIsNullPredicateExpression {
    override val token: KaLifetimeToken get() = backingArgument.token

    override val argument: KaContractParameterValue get() = withValidityAssertion { backingArgument }

    override val isNegated: Boolean get() = withValidityAssertion { backingIsNegated }

    override fun negated(): KaContractIsNullPredicateExpression =
        KaBaseContractIsNullPredicateExpression(argument, !isNegated)

    override fun equals(other: Any?): Boolean {
        return this === other ||
                other is KaBaseContractIsNullPredicateExpression &&
                other.backingArgument == backingArgument &&
                other.backingIsNegated == backingIsNegated
    }

    override fun hashCode(): Int = Objects.hashCode(backingArgument, backingIsNegated)
}
