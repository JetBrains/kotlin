// !DIAGNOSTICS: -INVISIBLE_MEMBER -INVISIBLE_REFERENCE
// !LANGUAGE: +CalledInPlaceEffect

import kotlin.internal.*

inline fun <T> myRun(@CalledInPlace(InvocationCount.EXACTLY_ONCE) block: () -> T): T = block()

fun functionWithExpressionBody(x: Int): Boolean = myRun {
    if (x == 0) return true
    if (x == 1) return false
    return functionWithExpressionBody(x - 2)
}