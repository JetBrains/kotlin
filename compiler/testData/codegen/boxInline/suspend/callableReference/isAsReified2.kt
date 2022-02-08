// WITH_STDLIB
// WITH_COROUTINES
// NO_CHECK_LAMBDA_INLINING
// FILE: 1.kt
package test

import kotlin.coroutines.*

inline fun <reified T> Any?.instanceOf() = this is T

inline suspend fun <reified T: suspend () -> String> T.callSuspend(): String = (this as (suspend () -> String))()

// FILE: 2.kt

import helpers.*
import test.*
import kotlin.coroutines.*

fun Any?.isSuspend(): Boolean = instanceOf<suspend () -> String>()
fun callMe(): String = "OK"
fun makeSuspend(x: suspend () -> String) = x

fun builder(c: suspend () -> Unit) {
    c.startCoroutine(EmptyContinuation)
}

fun box(): String {
    var res = "FAIL"
    builder {
        val adapted = makeSuspend(::callMe)
        res = if (adapted.isSuspend()) adapted.callSuspend() else "!isSuspend"
    }
    return res
}
