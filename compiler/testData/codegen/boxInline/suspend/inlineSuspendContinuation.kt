// IGNORE_BACKEND: JVM_IR
// IGNORE_BACKEND_MULTI_MODULE: JVM_IR
// FILE: test.kt
// COMMON_COROUTINES_TEST
// WITH_RUNTIME
// WITH_COROUTINES
// NO_CHECK_LAMBDA_INLINING
// TARGET_BACKEND: JVM

suspend inline fun test1(c: () -> Unit) {
    c()
}

suspend inline fun test2(c: suspend () -> Unit) {
    c()
}

suspend inline fun test3(crossinline c: suspend() -> Unit) {
    c()
}

suspend inline fun test4(crossinline c: suspend() -> Unit) {
    val l : suspend () -> Unit = { c() }
    l()
}

interface SuspendRunnable {
    suspend fun run()
}

suspend inline fun test5(crossinline c: suspend() -> Unit) {
    val sr = object : SuspendRunnable {
        override suspend fun run() {
            c()
        }
    }
    sr.run()
}

// FILE: box.kt
// COMMON_COROUTINES_TEST

import COROUTINES_PACKAGE.*
import COROUTINES_PACKAGE.intrinsics.*
import helpers.*
import COROUTINES_PACKAGE.jvm.internal.*


fun builder(c: suspend () -> Unit) {
    c.startCoroutine(EmptyContinuation)
}

var continuationChanged = true
var savedContinuation: Continuation<Unit>? = null

suspend inline fun saveContinuation() = suspendCoroutineUninterceptedOrReturn<Unit> { c ->
    savedContinuation = c
    Unit
}

suspend inline fun checkContinuation(continuation: Continuation<Unit>) = suspendCoroutineUninterceptedOrReturn<Unit> { c ->
    continuationChanged = (continuation !== c)
    Unit
}

fun box() : String {
    builder {
        saveContinuation()
        test1 {
            checkContinuation(savedContinuation!!)
        }
    }
    if (continuationChanged) return "FAIL 1"
    continuationChanged = true
    builder {
        saveContinuation()
        test2 {
            checkContinuation(savedContinuation!!)
        }
    }
    if (continuationChanged) return "FAIL 2"
    continuationChanged = true
    builder {
        saveContinuation()
        test3 {
            checkContinuation(savedContinuation!!)
        }
    }
    if (continuationChanged) return "FAIL 3"
    continuationChanged = false
    builder {
        saveContinuation()
        test4 {
            checkContinuation(savedContinuation!!)
        }
    }
    if (!continuationChanged) return "FAIL 4"
    continuationChanged = false
    builder {
        saveContinuation()
        test5 {
            checkContinuation(savedContinuation!!)
        }
    }
    if (continuationChanged) return "FAIL 5"
    return "OK"
}
