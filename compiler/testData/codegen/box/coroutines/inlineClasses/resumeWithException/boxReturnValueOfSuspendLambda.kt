// IGNORE_BACKEND: JS_IR_ES6
// WITH_STDLIB
// WITH_COROUTINES

import helpers.*
import kotlin.coroutines.*

inline class R(val x: Any)

var result: String = "fail"

fun builder(c: suspend () -> Unit) {
    c.startCoroutine(handleExceptionContinuation {
        result = it.message!!
    })
}

suspend fun <T> call(fn: suspend () -> T) = fn()

fun useR(r: R) = if (r.x == "OK") "OK" else "fail: $r"

fun box(): String {
    var c: Continuation<R>? = null
    builder {
        useR(call { suspendCoroutine { c = it } })
    }
    c?.resumeWithException(IllegalStateException("OK"))
    return result
}