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

var c: Continuation<R>? = null

suspend fun ok() = suspendCoroutine<R> { c = it }

fun box(): String {
    builder {
        useR(call(::ok))
    }
    c?.resumeWithException(IllegalStateException("OK"))
    return result
}