/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.lower.matchers

import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrStatementOrigin

internal interface IrCallMatcher : (IrCall) -> Boolean

/**
 * IrCallMatcher that puts restrictions only on its callee.
 */
internal class SimpleCalleeMatcher(
    restrictions: IrFunctionMatcherContainer.() -> Unit
) : IrCallMatcher {

    private val calleeRestriction: IrFunctionMatcher = createIrFunctionRestrictions(restrictions)

    override fun invoke(call: IrCall) = calleeRestriction(call.symbol.owner)
}

internal class IrCallExtensionReceiverMatcher(
    val restriction: (IrExpression?) -> Boolean
) : IrCallMatcher {
    override fun invoke(call: IrCall) = restriction(call.extensionReceiver)
}

internal class IrCallDispatchReceiverMatcher(
    val restriction: (IrExpression?) -> Boolean
) : IrCallMatcher {
    override fun invoke(call: IrCall) = restriction(call.dispatchReceiver)
}

internal class IrCallOriginMatcher(
    val restriction: (IrStatementOrigin?) -> Boolean
) : IrCallMatcher {
    override fun invoke(call: IrCall) = restriction(call.origin)
}

internal enum class Quantifier { ALL, ANY }

internal open class IrCallMatcherContainer(private val quantifier: Quantifier) : IrCallMatcher {

    private val matchers = mutableListOf<IrCallMatcher>()

    fun add(matcher: IrCallMatcher) {
        matchers += matcher
    }

    fun extensionReceiver(restriction: (IrExpression?) -> Boolean) =
        add(IrCallExtensionReceiverMatcher(restriction))

    fun origin(restriction: (IrStatementOrigin?) -> Boolean) =
        add(IrCallOriginMatcher(restriction))

    fun callee(restrictions: IrFunctionMatcherContainer.() -> Unit) {
        add(SimpleCalleeMatcher(restrictions))
    }

    fun dispatchReceiver(restriction: (IrExpression?) -> Boolean) =
        add(IrCallDispatchReceiverMatcher(restriction))

    override fun invoke(call: IrCall) = when (quantifier) {
        Quantifier.ALL -> matchers.all { it(call) }
        Quantifier.ANY -> matchers.any { it(call) }
    }
}

internal fun createIrCallMatcher(
    quantifier: Quantifier = Quantifier.ALL,
    restrictions: IrCallMatcherContainer.() -> Unit
) =
    IrCallMatcherContainer(quantifier).apply(restrictions)