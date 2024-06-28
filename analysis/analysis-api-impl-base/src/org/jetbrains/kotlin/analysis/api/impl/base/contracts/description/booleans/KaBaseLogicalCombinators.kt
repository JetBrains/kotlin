/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.contracts.description.booleans

import com.google.common.base.Objects
import org.jetbrains.kotlin.analysis.api.contracts.description.booleans.KaContractBinaryLogicExpression
import org.jetbrains.kotlin.analysis.api.contracts.description.booleans.KaContractBinaryLogicExpression.KaLogicOperation
import org.jetbrains.kotlin.analysis.api.contracts.description.booleans.KaContractBooleanExpression
import org.jetbrains.kotlin.analysis.api.contracts.description.booleans.KaContractLogicalNotExpression
import org.jetbrains.kotlin.analysis.api.lifetime.KaLifetimeToken
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion

class KaBaseContractBinaryLogicExpression(
    private val backingLeft: KaContractBooleanExpression,
    private val backingRight: KaContractBooleanExpression,
    private val backingOperation: KaLogicOperation
) : KaContractBinaryLogicExpression {
    init {
        check(left.token === right.token) { "$left and $right should have the same lifetime token" }
    }

    override val token: KaLifetimeToken get() = backingLeft.token

    override val left: KaContractBooleanExpression get() = withValidityAssertion { backingLeft }

    override val right: KaContractBooleanExpression get() = withValidityAssertion { backingRight }

    override val operation: KaLogicOperation get() = withValidityAssertion { backingOperation }

    override fun equals(other: Any?): Boolean {
        return this === other ||
                other is KaBaseContractBinaryLogicExpression &&
                other.backingLeft == backingLeft &&
                other.backingRight == backingRight &&
                other.backingOperation == backingOperation
    }

    override fun hashCode(): Int = Objects.hashCode(backingLeft, backingRight, backingOperation)
}

class KaBaseContractLogicalNotExpression(private val backingArgument: KaContractBooleanExpression) : KaContractLogicalNotExpression {
    override val token: KaLifetimeToken get() = backingArgument.token

    override val argument: KaContractBooleanExpression get() = withValidityAssertion { backingArgument }

    override fun equals(other: Any?): Boolean {
        return this === other || other is KaBaseContractLogicalNotExpression && other.backingArgument == backingArgument
    }

    override fun hashCode(): Int = backingArgument.hashCode()
}
