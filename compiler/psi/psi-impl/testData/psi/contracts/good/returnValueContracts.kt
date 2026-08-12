@file:OptIn(ExperimentalContracts::class)

import kotlin.contracts.*

inline fun <T> T.myAlso(block: (T) -> Unit): T {
    contract {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
        returnsParameter(this@myAlso)
    }
    block(this)
    return this
}

inline fun <T, R> T.myLet(block: (T) -> R): R {
    contract {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
        returnsResultOf(block)
    }
    return block(this)
}
