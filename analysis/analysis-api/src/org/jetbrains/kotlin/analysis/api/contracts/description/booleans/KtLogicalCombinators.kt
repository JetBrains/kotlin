/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.contracts.description.booleans

import com.google.common.base.Objects
import org.jetbrains.kotlin.analysis.api.lifetime.KaLifetimeToken
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion

/**
 * See: [KaContractBooleanExpression].
 */
public class KaContractBinaryLogicExpression(
    private val backingLeft: KaContractBooleanExpression,
    private val backingRight: KaContractBooleanExpression,
    private val backingOperation: KaLogicOperation
) : KaContractBooleanExpression {
    init {
        check(left.token === right.token) { "$left and $right should have the same lifetime token" }
    }

    override val token: KaLifetimeToken get() = backingLeft.token
    public val left: KaContractBooleanExpression get() = withValidityAssertion { backingLeft }
    public val right: KaContractBooleanExpression get() = withValidityAssertion { backingRight }
    public val operation: KaLogicOperation get() = withValidityAssertion { backingOperation }

    override fun hashCode(): Int = Objects.hashCode(backingLeft, backingRight, backingOperation)
    override fun equals(other: Any?): Boolean {
        return this === other ||
                other is KaContractBinaryLogicExpression &&
                other.backingLeft == backingLeft &&
                other.backingRight == backingRight &&
                other.backingOperation == backingOperation
    }

    public enum class KaLogicOperation {
        AND, OR
    }
}

public typealias KtContractBinaryLogicExpression = KaContractBinaryLogicExpression

/**
 * See: [KaContractBooleanExpression].
 */
public class KaContractLogicalNotExpression(private val backingArgument: KaContractBooleanExpression) : KaContractBooleanExpression {
    override val token: KaLifetimeToken get() = backingArgument.token
    public val argument: KaContractBooleanExpression get() = withValidityAssertion { backingArgument }

    override fun equals(other: Any?): Boolean {
        return this === other || other is KaContractLogicalNotExpression && other.backingArgument == backingArgument
    }

    override fun hashCode(): Int = backingArgument.hashCode()
}

public typealias KtContractLogicalNotExpression = KaContractLogicalNotExpression