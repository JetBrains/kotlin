// DONT_TARGET_EXACT_BACKEND: WASM
// WASM_MUTE_REASON: COROUTINES
// !LANGUAGE: +SuspendConversion
// WITH_RUNTIME
// WITH_COROUTINES

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