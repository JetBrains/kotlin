// WITH_COROUTINES
// NO_CHECK_LAMBDA_INLINING
// WITH_STDLIB
// FILE: inlined.kt
import kotlin.coroutines.*
import kotlin.coroutines.intrinsics.*
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
import kotlin.coroutines.*
import kotlin.coroutines.intrinsics.*
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
