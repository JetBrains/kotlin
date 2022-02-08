// WITH_STDLIB
// TARGET_BACKEND: JVM

// FILE: inline.kt

package test

import kotlin.coroutines.*
import kotlin.coroutines.intrinsics.*

fun runBlocking(c: suspend () -> Unit) {
    c.startCoroutine(Continuation(EmptyCoroutineContext){
        it.getOrThrow()
    })
}

inline fun <reified E> foo(noinline block: (String) -> Unit) = runBlocking {
    runCatching {
        runBlocking {
            var c: Continuation<Unit>? = null
            suspendCoroutineUninterceptedOrReturn<Unit> { c = it; Unit }
            block(c!!.toString())
        }
    }
    Unit
}

// FILE: test.kt

import test.*

fun box(): String {
    var res = "FAIL"
    foo<Exception> {
        res = it
    }
    return if (res.contains(".invokeSuspend(inline.kt:")) "OK" else res
}
