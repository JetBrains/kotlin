// IGNORE_BACKEND: NATIVE
// WITH_RUNTIME
// WITH_COROUTINES
import kotlin.coroutines.*

suspend fun suspendHere(): String = CoroutineIntrinsics.suspendCoroutineOrReturn { x ->
    x.resume("OK")
    CoroutineIntrinsics.SUSPENDED
}

fun builder(c: suspend () -> Unit) {
    c.startCoroutine(EmptyContinuation)
}

fun box(): String {
    var result = ""

    builder {
        result = suspendHere()
    }

    return result
}
