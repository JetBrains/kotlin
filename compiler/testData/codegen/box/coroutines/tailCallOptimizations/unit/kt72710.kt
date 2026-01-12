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

class Cont : Continuation<Unit> {
    override val context = EmptyCoroutineContext
    override fun resumeWith(result: Result<Unit>) {
        testResult = if (result == Result.success(Unit)) "OK" else "Bad: ${result}"
    }
}

fun box(): String {
    ::testUnit.startCoroutine(Cont())
    globalContinuation!!.resume(42)

    return testResult
}
