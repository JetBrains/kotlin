/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.contracts.description.booleans

import org.jetbrains.kotlin.analysis.api.contracts.description.booleans.KaContractBooleanConstantExpression
import org.jetbrains.kotlin.analysis.api.contracts.description.booleans.KaContractBooleanValueParameterExpression
import org.jetbrains.kotlin.analysis.api.lifetime.KaLifetimeToken
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.symbols.KaParameterSymbol

class KaBaseContractBooleanValueParameterExpression(
    private val backingParameterSymbol: KaParameterSymbol,
) : KaContractBooleanValueParameterExpression {
    override val token: KaLifetimeToken get() = backingParameterSymbol.token

    override val parameterSymbol: KaParameterSymbol get() = withValidityAssertion { backingParameterSymbol }

    override fun equals(other: Any?): Boolean {
        return this === other
                || other is KaBaseContractBooleanValueParameterExpression
                && other.backingParameterSymbol == backingParameterSymbol
    }

    override fun hashCode(): Int = backingParameterSymbol.hashCode()
}

class KaBaseContractBooleanConstantExpression(
    private val backingBooleanConstant: Boolean,
    override val token: KaLifetimeToken
) : KaContractBooleanConstantExpression {
    override val booleanConstant: Boolean get() = withValidityAssertion { backingBooleanConstant }

    override fun equals(other: Any?): Boolean {
        return this === other || other is KaBaseContractBooleanConstantExpression && other.backingBooleanConstant == backingBooleanConstant
    }

    override fun hashCode(): Int = backingBooleanConstant.hashCode()
}
