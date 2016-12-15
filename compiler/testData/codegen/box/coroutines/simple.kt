// IGNORE_BACKEND: JS
// WITH_RUNTIME
// WITH_COROUTINES
import kotlin.coroutines.*


suspend fun suspendHere(): String = suspendWithCurrentContinuation { x ->
    x.resume("OK")
    SUSPENDED
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
