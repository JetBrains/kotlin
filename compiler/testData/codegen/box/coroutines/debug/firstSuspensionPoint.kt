// This test depends on line numbers
// IGNORE_BACKEND_FIR: JVM_IR
// TARGET_BACKEND: JVM
// WITH_RUNTIME
// FULL_JDK
// WITH_COROUTINES
package test

import helpers.*
import kotlin.coroutines.*
import kotlin.coroutines.intrinsics.*

suspend fun foo() {
    /*
    A
    LOT
    OF
    EMPTY
    LINES
     */
    suspendHere()
    suspendHere()
}

val lambda: suspend () -> Unit = {
    /*
    A
    LOT
    OF
    EMPTY
    LINES
     */
    suspendHere()
    suspendHere()
}

var continuation: Continuation<Unit>? = null

suspend fun suspendHere() = suspendCoroutineUninterceptedOrReturn<Unit> {
    continuation = it
    COROUTINE_SUSPENDED
}

fun builder(c: suspend () -> Unit) {
    c.startCoroutine(EmptyContinuation)
}

fun box(): String {
    builder {
        foo()
    }
    if (!"$continuation".contains("21")) return "$continuation"
    continuation!!.resumeWith(Result.success(Unit))
    if (!"$continuation".contains("22")) return "$continuation"
    builder {
        lambda()
    }
    if (!"$continuation".contains("33")) return "$continuation"
    continuation!!.resumeWith(Result.success(Unit))
    if (!"$continuation".contains("34")) return "$continuation"
    return "OK"
}
