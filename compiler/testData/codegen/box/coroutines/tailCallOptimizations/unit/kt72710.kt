// TARGET_BACKEND: JVM
// FULL_JDK
// WITH_STDLIB
// WITH_COROUTINES

import helpers.*
import kotlin.coroutines.*
import kotlin.coroutines.intrinsics.*

var globalContinuation: Continuation<Int>? = null
var testResult: String = "NONE"

suspend fun sInt(): Int = suspendCoroutineUninterceptedOrReturn { x: Continuation<Int> ->
    globalContinuation = x
    COROUTINE_SUSPENDED
}

suspend fun testUnit(): Unit {
    sInt()
}

fun box(): String {
    ::testUnit.startCoroutine(Continuation(EmptyCoroutineContext) { result ->
        testResult = if (result == Result.success(Unit)) "OK" else "Bad: ${result}"
    })
    globalContinuation!!.resume(42)

    return testResult
}
