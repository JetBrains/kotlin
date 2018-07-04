// FILE: inlined.kt
// COMMON_COROUTINES_TEST
// WITH_RUNTIME
// WITH_COROUTINES
// NO_CHECK_LAMBDA_INLINING

import COROUTINES_PACKAGE.intrinsics.*
import helpers.*

var i = 0;

suspend fun suspendHere() = suspendCoroutineUninterceptedOrReturn<Unit> {
    i++
    COROUTINE_SUSPENDED
}

suspend inline fun crossinlineMe(crossinline c: suspend () -> Unit) {
    val l: suspend () -> Unit = {
        suspendHere()
        suspendHere()
        suspendHere()
        suspendHere()
        suspendHere()
    }
    l()
}

// FILE: inlineSite.kt
// COMMON_COROUTINES_TEST

import COROUTINES_PACKAGE.*
import helpers.*

fun builder(c: suspend () -> Unit) {
    c.startCoroutine(EmptyContinuation)
}

fun box(): String {
    var res = "OK"
    builder {
        crossinlineMe {
            res = "FAIL 1"
        }
    }
    if (i != 1) return "FAIL 2"
    return res
}
