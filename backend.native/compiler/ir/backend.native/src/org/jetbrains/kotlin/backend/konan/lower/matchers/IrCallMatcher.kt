package org.jetbrains.kotlin.backend.konan.lower.matchers

import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression


internal interface IrCallMatcher : (IrCall) -> Boolean

/**
 * IrCallMatcher that puts restrictions only on its callee.
 */
internal class SimpleCalleeMatcher(
        restrictions: IrFunctionMatcherContainer.() -> Unit
) : IrCallMatcher {

    private val calleeRestriction: IrFunctionMatcher
            = createIrFunctionRestrictions(restrictions)

    override fun invoke(call: IrCall) = calleeRestriction(call.symbol.owner)
}

internal class IrCallExtensionReceiverMatcher(
        val restriction: (IrExpression?) -> Boolean
) : IrCallMatcher {
    override fun invoke(call: IrCall) = restriction(call.extensionReceiver)
}

internal open class IrCallMatcherContainer : IrCallMatcher {

    private val matchers = mutableListOf<IrCallMatcher>()

    fun add(matcher: IrCallMatcher) {
        matchers += matcher
    }

    fun extensionReceiver(restriction: (IrExpression?) -> Boolean) =
            add(IrCallExtensionReceiverMatcher(restriction))

    fun callee(restrictions: IrFunctionMatcherContainer.() -> Unit) {
        add(SimpleCalleeMatcher(restrictions))
    }

    override fun invoke(call: IrCall) = matchers.all { it(call) }
}

internal fun createIrCallMatcher(restrictions: IrCallMatcherContainer.() -> Unit) =
        IrCallMatcherContainer().apply(restrictions)