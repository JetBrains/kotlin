// !LANGUAGE: +CallsInPlaceEffect
// !DIAGNOSTICS: -INVISIBLE_REFERENCE -INVISIBLE_MEMBER

import kotlin.internal.contracts.*

inline fun <T> myRun(block: () -> T): T {
    contract {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
    }
    return block()
}

fun functionWithExpressionBody(x: Int): Boolean = myRun {
    if (x == 0) return true
    if (x == 1) return false
    return functionWithExpressionBody(x - 2)
}