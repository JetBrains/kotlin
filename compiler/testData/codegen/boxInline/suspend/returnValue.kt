// FILE: inlined.kt
// COMMON_COROUTINES_TEST
// WITH_RUNTIME
// WITH_COROUTINES
// NO_CHECK_LAMBDA_INLINING
import COROUTINES_PACKAGE.*
import COROUTINES_PACKAGE.intrinsics.*
import helpers.*

inline suspend fun suspendThere(v: String): String = suspendCoroutineUninterceptedOrReturn { x ->
    x.resume(v)
    COROUTINE_SUSPENDED
}

suspend inline fun complexSuspend(crossinline c: suspend () -> String): String {
    return run {
        c()
    }
}

// FILE: inleneSite.kt
// COMMON_COROUTINES_TEST
import COROUTINES_PACKAGE.*
import COROUTINES_PACKAGE.intrinsics.*
import helpers.*

fun builder(c: suspend () -> Unit) {
    c.startCoroutine(EmptyContinuation)
}

suspend fun suspendHere(): String = suspendThere("O")

fun box(): String {
    var result = ""

    builder {
        result = suspendHere() + complexSuspend { suspendThere("K") }
    }

    return result
}
