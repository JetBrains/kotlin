/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.contracts.description.booleans

import com.google.common.base.Objects
import org.jetbrains.kotlin.analysis.api.lifetime.KtLifetimeToken
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion

/**
 * See: [KtContractBooleanExpression].
 */
public class KtContractBinaryLogicExpression(
    private val backingLeft: KtContractBooleanExpression,
    private val backingRight: KtContractBooleanExpression,
    private val backingOperation: KtLogicOperation
) : KtContractBooleanExpression {
    init {
        check(left.token === right.token) { "$left and $right should have the same lifetime token" }
    }

    override val token: KtLifetimeToken get() = backingLeft.token
    public val left: KtContractBooleanExpression get() = withValidityAssertion { backingLeft }
    public val right: KtContractBooleanExpression get() = withValidityAssertion { backingRight }
    public val operation: KtLogicOperation get() = withValidityAssertion { backingOperation }

    override fun hashCode(): Int = Objects.hashCode(backingLeft, backingRight, backingOperation)
    override fun equals(other: Any?): Boolean {
        return this === other ||
                other is KtContractBinaryLogicExpression &&
                other.backingLeft == backingLeft &&
                other.backingRight == backingRight &&
                other.backingOperation == backingOperation
    }

    public enum class KtLogicOperation {
        AND, OR
    }
}

/**
 * See: [KtContractBooleanExpression].
 */
public class KtContractLogicalNotExpression(private val backingArgument: KtContractBooleanExpression) : KtContractBooleanExpression {
    override val token: KtLifetimeToken get() = backingArgument.token
    public val argument: KtContractBooleanExpression get() = withValidityAssertion { backingArgument }

    override fun equals(other: Any?): Boolean {
        return this === other || other is KtContractLogicalNotExpression && other.backingArgument == backingArgument
    }

    override fun hashCode(): Int = backingArgument.hashCode()
}
