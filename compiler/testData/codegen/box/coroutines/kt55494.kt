// WITH_STDLIB
// WITH_COROUTINES

// FILE: t1.kt
import kotlin.coroutines.intrinsics.COROUTINE_SUSPENDED
import kotlin.coroutines.intrinsics.suspendCoroutineUninterceptedOrReturn
import kotlin.coroutines.*

private suspend fun suspendHere(): Int = suspendCoroutineUninterceptedOrReturn { x ->
    x.resume(42)
    COROUTINE_SUSPENDED
}

suspend fun foo() = suspendHere() + suspendHere()

// FILE: t2.kt
import kotlin.coroutines.intrinsics.COROUTINE_SUSPENDED
import kotlin.coroutines.intrinsics.suspendCoroutineUninterceptedOrReturn
import kotlin.coroutines.*

private suspend fun suspendHere(): Int = suspendCoroutineUninterceptedOrReturn { x ->
    x.resume(42)
    COROUTINE_SUSPENDED
}

suspend fun foo(x: Int) = suspendHere() - x

// FILE: main.kt
import helpers.*

import kotlin.coroutines.*

fun builder(c: suspend () -> Unit) {
    c.startCoroutine(EmptyContinuation)
}

fun box(): String {
    var result = 0

    builder {
        result += foo()
        result += foo(9)
    }

    return if (result == 117) "OK" else "fail"
}
