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

var c: Continuation<Any>? = null

fun box(): String {
    val lambda: suspend (IC, IC) -> String = { _, _ ->
        suspendCoroutine<String> {
            @Suppress("UNCHECKED_CAST")
            c = it as Continuation<Any>
        }
    }
    builder {
        lambda(IC("O"), IC("K"))
    }
    c?.resumeWithException(IllegalStateException("OK"))
    return result
}