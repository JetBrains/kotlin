// IGNORE_BACKEND: JVM_IR
// WITH_RUNTIME
// WITH_COROUTINES
// COMMON_COROUTINES_TEST

import helpers.*
import COROUTINES_PACKAGE.*
import COROUTINES_PACKAGE.intrinsics.*

suspend fun suspendHere(): String {
    var z = "fail1"
    suspendCoroutineUninterceptedOrReturn<Unit> { x ->
        z = "OK"
        x.resume(Unit)
        COROUTINE_SUSPENDED
    }
    return z
}

fun builder(c: suspend () -> Unit) {
    c.startCoroutine(EmptyContinuation)
}

fun box(): String {
    var result = "fail2"

    builder {
        result = suspendHere()
    }

    return result
}