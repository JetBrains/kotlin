// !LANGUAGE: +SuspendConversion
// WITH_RUNTIME
// WITH_COROUTINES
// IGNORE_BACKEND_FIR: JVM_IR
// IGNORE_BACKEND: JS

import helpers.*
import kotlin.coroutines.*
import kotlin.coroutines.intrinsics.*


fun runSuspend(c: suspend () -> Unit) {
    c.startCoroutine(EmptyContinuation)
}

var test = "failed"

fun foo() { test = "OK" }

inline suspend fun invokeSuspend(fn: suspend () -> Unit) { fn() }

fun box(): String {
    runSuspend {
        invokeSuspend(::foo)
    }
    return test
}