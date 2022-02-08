// WITH_STDLIB
// WITH_COROUTINES

import helpers.*
import kotlin.coroutines.*
import kotlin.coroutines.intrinsics.*

suspend fun pause(): Unit = suspendCoroutineUninterceptedOrReturn { x ->
    x.resume(Unit)
    COROUTINE_SUSPENDED
}

interface I {
    suspend fun f()
}

var unpaused = false

class A : I {
    override suspend fun f() {
        pause()
        unpaused = true
    }
}

class B(val x: I) : I by x

fun box(): String {
    suspend {
        B(A()).f()
    }.startCoroutine(EmptyContinuation)
    return if (unpaused) "OK" else "fail: ignored suspension"
}
