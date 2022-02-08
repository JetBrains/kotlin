// WITH_STDLIB
// WITH_COROUTINES
// CHECK_BYTECODE_LISTING
// CHECK_NEW_COUNT: function=suspendHere count=0
// FIXME: Coroutine inlining
// CHECK_NEW_COUNT: function=complexSuspend count=0 TARGET_BACKENDS=JS
import helpers.*
import kotlin.coroutines.*
import kotlin.coroutines.intrinsics.*

inline suspend fun suspendThere(v: String): String = suspendCoroutineUninterceptedOrReturn { x ->
    x.resume(v)
    COROUTINE_SUSPENDED
}

suspend fun suspendHere(): String = suspendThere("O")

suspend fun complexSuspend(): String {
    return run {
        suspendThere("K")
    }
}

fun builder(c: suspend () -> Unit) {
    c.startCoroutine(EmptyContinuation)
}

fun box(): String {
    var result = ""

    builder {
        result = suspendHere() + complexSuspend()
    }

    return result
}
