// WITH_STDLIB
import kotlin.coroutines.*
import kotlin.coroutines.intrinsics.*

var c: Continuation<Unit>? = null

suspend fun suspendHere(): Unit = suspendCoroutineUninterceptedOrReturn { x ->
    c = x
    COROUTINE_SUSPENDED
}

// Starts a new coroutine whose completion is the continuation of the current coroutine.
suspend fun startInner(): String = suspendCoroutineUninterceptedOrReturn { cont ->
    val inner: suspend () -> String = {
        suspendHere()
        "OK"
    }
    inner.startCoroutineUninterceptedOrReturn(cont)
}

fun box(): String {
    var result: Result<String>? = null
    val outer: suspend () -> String = { startInner() }
    outer.startCoroutine(Continuation(EmptyCoroutineContext) { result = it })

    if (result != null) return "fail 1: completed before resume: $result"

    // Resumes the inner coroutine; its completion must resume the outer one with "OK".
    c!!.resume(Unit)
    val r = result ?: return "fail 2: outer coroutine is not completed"
    return r.getOrThrow()
}
