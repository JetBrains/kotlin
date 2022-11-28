/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.contracts.description.booleans

import org.jetbrains.kotlin.analysis.api.lifetime.KtLifetimeOwner
import org.jetbrains.kotlin.analysis.api.lifetime.KtLifetimeToken
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.symbols.KtParameterSymbol

/**
 * Represents `booleanExpression` argument of [kotlin.contracts.SimpleEffect.implies].
 *
 * `booleanExpression` forms a boolean condition for
 * [org.jetbrains.kotlin.analysis.api.contracts.description.KtContractConditionalContractEffectDeclaration]. See
 * [org.jetbrains.kotlin.analysis.api.contracts.description.KtContractConditionalContractEffectDeclaration.condition]
 */
public sealed interface KtContractBooleanExpression : KtLifetimeOwner

/**
 * Represents boolean parameter reference passed to `booleanExpression` argument of [kotlin.contracts.SimpleEffect.implies].
 */
public class KtContractBooleanValueParameterExpression(
    private val _parameterSymbol: KtParameterSymbol
) : KtContractBooleanExpression {
    override val token: KtLifetimeToken get() = _parameterSymbol.token
    public val parameterSymbol: KtParameterSymbol get() = withValidityAssertion { _parameterSymbol }
    override fun hashCode(): Int = _parameterSymbol.hashCode()
    override fun equals(other: Any?): Boolean =
        other is KtContractBooleanValueParameterExpression && other._parameterSymbol == _parameterSymbol
}

/**
 * Represents boolean constant reference. The boolean constant can be passed to `booleanExpression` argument of
 * [kotlin.contracts.SimpleEffect.implies].
 */
public class KtContractBooleanConstantExpression(
    private val _booleanConstant: Boolean,
    override val token: KtLifetimeToken
) : KtContractBooleanExpression {
    public val booleanConstant: Boolean get() = withValidityAssertion { _booleanConstant }

    override fun equals(other: Any?): Boolean = other is KtContractBooleanConstantExpression && other._booleanConstant == _booleanConstant
    override fun hashCode(): Int = _booleanConstant.hashCode()
}
