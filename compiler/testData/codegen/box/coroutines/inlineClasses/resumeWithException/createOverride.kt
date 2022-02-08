// WITH_STDLIB
// WITH_COROUTINES

import kotlin.coroutines.*
import helpers.*

var result = "FAIL"

fun builder(c: suspend () -> Unit) {
    c.startCoroutine(handleExceptionContinuation {
        result = it.message!!
    })
}

inline class IC(val s: String)

suspend fun <T> List<T>.onEach(c: suspend (T) -> Unit) {
    for (e in this) {
        c(e)
    }
}

var c: Continuation<Any>? = null

fun box(): String {
    builder {
        listOf(IC("O"), IC("K")).onEach { suspendCoroutine<String> { cont ->
            @Suppress("UNCHECKED_CAST")
            c = cont as Continuation<Any>
        }}
    }
    c?.resumeWithException(IllegalStateException("OK"))
    return result
}