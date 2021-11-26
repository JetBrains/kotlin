// !LANGUAGE: +SuspendConversion
// WITH_STDLIB
// WITH_COROUTINES

import helpers.*
import kotlin.coroutines.*
import kotlin.coroutines.intrinsics.*

fun runSuspend(c: suspend () -> Unit) {
    c.startCoroutine(EmptyContinuation)
}

fun foo(s: String): String = s + "K"

inline suspend fun invokeSuspend(fn: suspend (String) -> String, arg: String) = fn.invoke(arg)

fun box(): String {
    var test = "failed"
    runSuspend {
        test = invokeSuspend(::foo, "O")
    }
    return test
}