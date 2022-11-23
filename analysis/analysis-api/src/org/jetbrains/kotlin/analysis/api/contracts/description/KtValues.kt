/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.contracts.description

import org.jetbrains.kotlin.analysis.api.lifetime.KtLifetimeToken
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion

/**
 * Represents `value` argument of [kotlin.contracts.ContractBuilder.returns] and [kotlin.contracts.ContractBuilder.returnsNotNull].
 * The `value` can be either be a constant reference [KtContractConstantReference], or a parameter reference
 * [KtContractAbstractValueParameterReference].
 *
 * * K1: [org.jetbrains.kotlin.contracts.description.expressions.ContractDescriptionValue]
 * * K2: [org.jetbrains.kotlin.fir.contracts.description.ConeContractDescriptionValue]
 */
public sealed class KtContractDescriptionValue(override val token: KtLifetimeToken) : KtContractDescriptionElement

/**
 * Represents constant reference passed to `value` argument of [kotlin.contracts.ContractBuilder.returns] or
 * [kotlin.contracts.ContractBuilder.returnsNotNull]. Also see: [KtContractDescriptionValue].
 *
 * * K1: [org.jetbrains.kotlin.contracts.description.expressions.ConstantReference]
 * * K2: [org.jetbrains.kotlin.fir.contracts.description.ConeConstantReference]
 */
public sealed class KtContractConstantReference(token: KtLifetimeToken) : KtContractDescriptionValue(token) {
    /**
     * Represents [kotlin.contracts.ContractBuilder.returns] with `null` argument.
     */
    public class KtNull(token: KtLifetimeToken) : KtContractConstantReference(token)

    /**
     * Represents [kotlin.contracts.ContractBuilder.returns] without arguments.
     */
    public class KtWildcard(token: KtLifetimeToken) : KtContractConstantReference(token)

    /**
     * Represents [kotlin.contracts.ContractBuilder.returnsNotNull].
     */
    public class KtNotNull(token: KtLifetimeToken) : KtContractConstantReference(token)

    /**
     * Represents boolean constant reference passed to `value` argument of [kotlin.contracts.ContractBuilder.returns] or
     * [kotlin.contracts.ContractBuilder.returnsNotNull]. Also see: [KtContractDescriptionValue].
     *
     * * K1: [org.jetbrains.kotlin.contracts.description.expressions.BooleanConstantReference]
     * * K2: [org.jetbrains.kotlin.fir.contracts.description.ConeBooleanConstantReference]
     */
    public sealed class KtContractBooleanConstantReference(token: KtLifetimeToken) :
        KtContractConstantReference(token), KtContractBooleanExpression {
        public class KtTrue(token: KtLifetimeToken) : KtContractBooleanConstantReference(token)
        public class KtFalse(token: KtLifetimeToken) : KtContractBooleanConstantReference(token)
    }
}

public sealed class KtContractAbstractValueParameterReference(
    private val _parameterIndex: Int,
    private val _name: String,
    token: KtLifetimeToken
) : KtContractDescriptionValue(token) {
    public val parameterIndex: Int get() = withValidityAssertion { _parameterIndex }
    public val name: String get() = withValidityAssertion { _name }

    /**
     * Represents parameter reference passed to `value` argument of [kotlin.contracts.ContractBuilder.returns] or
     * [kotlin.contracts.ContractBuilder.returnsNotNull]. Also see: [KtContractDescriptionValue].
     *
     * * K1: [org.jetbrains.kotlin.contracts.description.expressions.VariableReference]
     * * K2: [org.jetbrains.kotlin.fir.contracts.description.ConeValueParameterReference]
     */
    public class KtContractValueParameterReference(parameterIndex: Int, name: String, token: KtLifetimeToken) :
        KtContractAbstractValueParameterReference(parameterIndex, name, token)

    /**
     * Represents boolean parameter reference passed to `value` argument of [kotlin.contracts.ContractBuilder.returns] or
     * [kotlin.contracts.ContractBuilder.returnsNotNull]. Also see: [KtContractDescriptionValue].
     *
     * * K1: [org.jetbrains.kotlin.contracts.description.expressions.BooleanVariableReference]
     * * K2: [org.jetbrains.kotlin.fir.contracts.description.ConeBooleanValueParameterReference]
     */
    public class KtContractBooleanValueParameterReference(parameterIndex: Int, name: String, token: KtLifetimeToken) :
        KtContractAbstractValueParameterReference(parameterIndex, name, token), KtContractBooleanExpression
}
