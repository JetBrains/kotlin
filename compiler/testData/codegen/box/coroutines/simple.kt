// WITH_STDLIB
// WITH_COROUTINES
import helpers.*
import kotlin.coroutines.*
import kotlin.coroutines.intrinsics.*

suspend fun suspendHere(): Int = suspendCoroutineUninterceptedOrReturn { x ->
    x.resume(42)
    COROUTINE_SUSPENDED
}

fun builder(c: suspend () -> Unit) {
    c.startCoroutine(EmptyContinuation)
}

fun box(): Boolean {
    var result = -1

    builder {
        result = suspendHere()
    }

    return result == 42
}
