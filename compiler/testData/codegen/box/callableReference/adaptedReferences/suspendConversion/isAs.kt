// !LANGUAGE: +SuspendConversion
// WITH_STDLIB
// WITH_COROUTINES

import helpers.*
import kotlin.coroutines.*

fun callMe(): String = "OK"

suspend fun isSuspend(c: suspend () -> String) = c is suspend () -> String
suspend fun callSuspend(c: suspend () -> String) = (c as suspend () -> String)()

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