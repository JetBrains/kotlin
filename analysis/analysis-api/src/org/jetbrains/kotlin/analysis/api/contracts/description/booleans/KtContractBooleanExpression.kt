/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.contracts.description.booleans

import org.jetbrains.kotlin.analysis.api.lifetime.KaLifetimeOwner
import org.jetbrains.kotlin.analysis.api.lifetime.KaLifetimeToken
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.symbols.KaParameterSymbol

/**
 * Represents `booleanExpression` argument of [kotlin.contracts.SimpleEffect.implies].
 *
 * `booleanExpression` forms a boolean condition for
 * [org.jetbrains.kotlin.analysis.api.contracts.description.KaContractConditionalContractEffectDeclaration]. See
 * [org.jetbrains.kotlin.analysis.api.contracts.description.KaContractConditionalContractEffectDeclaration.condition]
 */
public sealed interface KaContractBooleanExpression : KaLifetimeOwner

public typealias KtContractBooleanExpression = KaContractBooleanExpression

/**
 * Represents boolean parameter reference passed to `booleanExpression` argument of [kotlin.contracts.SimpleEffect.implies].
 */
public class KaContractBooleanValueParameterExpression(
    private val backingParameterSymbol: KaParameterSymbol
) : KaContractBooleanExpression {
    override val token: KaLifetimeToken get() = backingParameterSymbol.token
    public val parameterSymbol: KaParameterSymbol get() = withValidityAssertion { backingParameterSymbol }
    override fun hashCode(): Int = backingParameterSymbol.hashCode()
    override fun equals(other: Any?): Boolean {
        return this === other || other is KaContractBooleanValueParameterExpression && other.backingParameterSymbol == backingParameterSymbol
    }
}

public typealias KtContractBooleanValueParameterExpression = KaContractBooleanValueParameterExpression

/**
 * Represents boolean constant reference. The boolean constant can be passed to `booleanExpression` argument of
 * [kotlin.contracts.SimpleEffect.implies].
 */
public class KaContractBooleanConstantExpression(
    private val backingBooleanConstant: Boolean,
    override val token: KaLifetimeToken
) : KaContractBooleanExpression {
    public val booleanConstant: Boolean get() = withValidityAssertion { backingBooleanConstant }

    override fun equals(other: Any?): Boolean {
        return this === other || other is KaContractBooleanConstantExpression && other.backingBooleanConstant == backingBooleanConstant
    }

    override fun hashCode(): Int = backingBooleanConstant.hashCode()
}

public typealias KtContractBooleanConstantExpression = KaContractBooleanConstantExpression