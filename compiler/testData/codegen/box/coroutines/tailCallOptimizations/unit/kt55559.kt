// TARGET_BACKEND: JVM
// FULL_JDK
// WITH_STDLIB
// WITH_COROUTINES

import helpers.*
import kotlin.coroutines.*
import kotlin.coroutines.intrinsics.*

var globalContinuation: Continuation<Long>? = null
var testResult: Any = "NONE"

class Foo {
    suspend fun bar1() {
        foo()
    }

    suspend fun foo(): Long = suspendCoroutineUninterceptedOrReturn { x ->
        globalContinuation = x
        COROUTINE_SUSPENDED
    }
}

suspend fun <T> process(fn: suspend () -> T, fn2: (T) -> Unit) {
    fn2(fn())
}

fun builder(c: suspend () -> Unit) {
    c.startCoroutine(EmptyContinuation)
}

fun box(): String {
    builder {
        process(Foo()::bar1) { testResult = it }
    }
    globalContinuation!!.resume(1000L)
    return if (testResult == Unit) "OK" else "Bad result: $testResult"
}
