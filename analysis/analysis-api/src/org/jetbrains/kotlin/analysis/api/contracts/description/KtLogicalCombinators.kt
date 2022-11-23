/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.contracts.description

import org.jetbrains.kotlin.analysis.api.lifetime.KtLifetimeToken
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion

/**
 * K1: [org.jetbrains.kotlin.contracts.description.expressions.LogicalOr] & [org.jetbrains.kotlin.contracts.description.expressions.LogicalAnd]
 * K2: [org.jetbrains.kotlin.fir.contracts.description.ConeBinaryLogicExpression]
 */
public class KtContractBinaryLogicExpression(
    private val _left: KtContractBooleanExpression,
    private val _right: KtContractBooleanExpression,
    private val _kind: KtLogicOperationKind
) : KtContractBooleanExpression {
    init {
        check(left.token === right.token) { "$left and $right should have the same lifetime token" }
    }

    override val token: KtLifetimeToken get() = _left.token
    public val left: KtContractBooleanExpression get() = withValidityAssertion { _left }
    public val right: KtContractBooleanExpression get() = withValidityAssertion { _right }
    public val kind: KtLogicOperationKind get() = withValidityAssertion { _kind }

    public enum class KtLogicOperationKind(public val token: String) {
        AND("&&"), OR("||")
    }
}

/**
 * K1: [org.jetbrains.kotlin.contracts.description.expressions.LogicalNot]
 * K2: [org.jetbrains.kotlin.fir.contracts.description.ConeLogicalNot]
 */
public class KtContractLogicalNot(private val _argument: KtContractBooleanExpression) : KtContractBooleanExpression {
    override val token: KtLifetimeToken get() = _argument.token
    public val argument: KtContractBooleanExpression get() = withValidityAssertion { _argument }
}
