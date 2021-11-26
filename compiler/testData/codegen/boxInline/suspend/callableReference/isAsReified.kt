// WITH_STDLIB
// WITH_COROUTINES
// NO_CHECK_LAMBDA_INLINING
// FILE: 1.kt
package test

import kotlin.coroutines.*

suspend fun callMe(): String = "OK"

inline suspend fun <reified T> isSuspend(t: T): Boolean = t is (SuspendFunction0<*>)

inline suspend fun <reified T> callSuspend(t: T): String = (t as (suspend () -> String))()

// FILE: 2.kt

import helpers.*
import test.*
import kotlin.coroutines.*

fun builder(c: suspend () -> Unit) {
    c.startCoroutine(EmptyContinuation)
}

fun box(): String {
    var res = "FAIL"
    builder {
        res = if (isSuspend(::callMe)) callSuspend(::callMe) else "!isSuspend"
    }
    return res
}
