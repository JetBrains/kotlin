// WITH_STDLIB
// WITH_COROUTINES
import helpers.*
import kotlin.coroutines.*
import kotlin.coroutines.intrinsics.*

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
