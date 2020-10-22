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

fun foo(s: String): String {
    test = s + "K"
    return test
}

inline suspend fun invokeSuspend(fn: suspend (String) -> Unit, arg: String) = fn.invoke(arg)

fun box(): String {
    runSuspend {
        invokeSuspend(::foo, "O")
    }
    return test
}