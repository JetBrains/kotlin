// TARGET_BACKEND: JVM
// JVM_TARGET: 1.8
// LAMBDAS: INDY
// WITH_RUNTIME
// WITH_COROUTINES

import kotlin.coroutines.*

var c: Continuation<Unit>? = null

suspend fun suspendMe() = suspendCoroutine<Unit> { continuation ->
    c = continuation
}

fun builder(c: suspend () -> Unit) {
    c.startCoroutine(object: Continuation<Unit> {
        override val context = EmptyCoroutineContext
        override fun resumeWith(result: Result<Unit>) {
            result.getOrThrow()
        }
    })
}

fun box(): String {
    var test = ""
    builder {
        suspendMe()
        test += "O"
        suspendMe()
        test += "K"
    }
    c?.resume(Unit)
    c?.resume(Unit)
    return test
}
