// TARGET_BACKEND: JVM_IR
// WITH_STDLIB
// WITH_COROUTINES

import helpers.*
import kotlin.coroutines.*
import kotlin.coroutines.intrinsics.*

var c: Continuation<Unit>? = null

suspend fun delay() = suspendCoroutine {
    c = it
    COROUTINE_SUSPENDED
}

class Foo {
    suspend fun bar() {
        baz()
    }

    suspend fun baz(): Long {
        delay()
        return 1000L
    }
}

suspend fun <T> process(fn: suspend () -> T): String {
    val r = fn()
    return if (r is Unit) "OK" else "FAIL"
}

fun box(): String {
    var result = ""
    suspend {
        result = process(Foo()::bar)
    }.startCoroutine(EmptyContinuation)
    c?.resume(Unit)
    return result
}