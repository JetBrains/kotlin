// !LANGUAGE: +SuspendConversion
// WITH_RUNTIME
// WITH_COROUTINES
// IGNORE_BACKEND_FIR: JVM_IR

import helpers.*
import kotlin.coroutines.*
import kotlin.coroutines.intrinsics.*

fun runSuspend(c: suspend () -> Unit) {
    c.startCoroutine(EmptyContinuation)
}

var test = "failed"

fun foo() { test = "OK" }

inline suspend fun invokeSuspend(crossinline fn: suspend () -> Unit) = suspend { fn() }

fun box(): String {
    runSuspend {
        invokeSuspend(::foo)()
    }
    return test
}