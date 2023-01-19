// WITH_STDLIB
// IGNORE_BACKEND: JVM

import kotlin.coroutines.*
import kotlin.coroutines.intrinsics.*

var c: Continuation<Long>? = null

class Foo {
    suspend fun bar1() {
        foo()
    }

    suspend fun foo(): Long = suspendCoroutineUninterceptedOrReturn { x ->
        c = x
        COROUTINE_SUSPENDED
    }
}

suspend fun <T> process(fn: suspend () -> T, fn2: (T) -> Unit) {
    fn2(fn())
}

fun builder(c: suspend () -> Unit) {
    c.startCoroutine(Continuation(EmptyCoroutineContext) {
        it.getOrThrow()
    })
}

fun box(): String {
    builder { process(Foo()::bar1) {} }
    c?.resumeWith(Result.success(1000L))

    return "OK"
}