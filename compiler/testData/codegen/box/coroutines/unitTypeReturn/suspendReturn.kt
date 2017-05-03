// WITH_RUNTIME
// WITH_COROUTINES
import helpers.*
import kotlin.coroutines.experimental.*
import kotlin.coroutines.experimental.intrinsics.*

var result = "0"

suspend fun suspendHere(x: Int): Unit {
    if (x == 0) return
    result = "OK"
    return suspendCoroutineOrReturn { x ->
        x.resume(Unit)
        COROUTINE_SUSPENDED
    }
}

fun builder(c: suspend () -> Unit) {
    c.startCoroutine(EmptyContinuation)
}

fun box(): String {
    builder {
        suspendHere(0)
        suspendHere(1)
    }

    return result
}
