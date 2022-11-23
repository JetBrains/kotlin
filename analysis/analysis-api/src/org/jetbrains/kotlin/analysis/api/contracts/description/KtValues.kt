/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.contracts.description

import org.jetbrains.kotlin.analysis.api.lifetime.KtLifetimeToken
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion

/**
 * K1: [org.jetbrains.kotlin.contracts.description.expressions.ContractDescriptionValue]
 * K2: [org.jetbrains.kotlin.fir.contracts.description.ConeContractDescriptionValue]
 */
public sealed class KtContractDescriptionValue(
    private val _name: String,
    override val token: KtLifetimeToken
) : KtContractDescriptionElement {
    public val name: String get() = withValidityAssertion { _name }
}

/**
 * K1: [org.jetbrains.kotlin.contracts.description.expressions.ConstantReference]
 * K2: [org.jetbrains.kotlin.fir.contracts.description.ConeConstantReference]
 */
public sealed class KtContractConstantReference(name: String, token: KtLifetimeToken) : KtContractDescriptionValue(name, token) {
    public class KtNull(token: KtLifetimeToken) : KtContractConstantReference("NULL", token)
    public class KtWildcard(token: KtLifetimeToken) : KtContractConstantReference("WILDCARD", token)
    public class KtNotNull(token: KtLifetimeToken) : KtContractConstantReference("NOT_NULL", token)

    /**
     * K1: [org.jetbrains.kotlin.contracts.description.expressions.BooleanConstantReference]
     * K2: [org.jetbrains.kotlin.fir.contracts.description.ConeBooleanConstantReference]
     */
    public sealed class KtContractBooleanConstantReference(name: String, token: KtLifetimeToken) :
        KtContractConstantReference(name, token), KtContractBooleanExpression {
        public class KtTrue(token: KtLifetimeToken) : KtContractBooleanConstantReference("TRUE", token)
        public class KtFalse(token: KtLifetimeToken) : KtContractBooleanConstantReference("FALSE", token)
    }
}

public sealed class KtContractAbstractValueParameterReference(
    private val _parameterIndex: Int,
    name: String,
    token: KtLifetimeToken
) : KtContractDescriptionValue(name, token) {
    public val parameterIndex: Int get() = withValidityAssertion { _parameterIndex }

    /**
     * K1: [org.jetbrains.kotlin.contracts.description.expressions.VariableReference]
     * K2: [org.jetbrains.kotlin.fir.contracts.description.ConeValueParameterReference]
     */
    public class KtContractValueParameterReference(parameterIndex: Int, name: String, token: KtLifetimeToken) :
        KtContractAbstractValueParameterReference(parameterIndex, name, token)

    /**
     * K1: [org.jetbrains.kotlin.contracts.description.expressions.BooleanVariableReference]
     * K2: [org.jetbrains.kotlin.fir.contracts.description.ConeBooleanValueParameterReference]
     */
    public class KtContractBooleanValueParameterReference(parameterIndex: Int, name: String, token: KtLifetimeToken) :
        KtContractAbstractValueParameterReference(parameterIndex, name, token), KtContractBooleanExpression
}
