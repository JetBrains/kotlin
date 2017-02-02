// WITH_RUNTIME
// WITH_COROUTINES
import kotlin.coroutines.*

var result = "0"

suspend fun suspendHere(x: Int): Unit {
    if (x == 0) return
    result = "OK"
    return CoroutineIntrinsics.suspendCoroutineOrReturn { x ->
        x.resume(Unit)
        CoroutineIntrinsics.SUSPENDED
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
