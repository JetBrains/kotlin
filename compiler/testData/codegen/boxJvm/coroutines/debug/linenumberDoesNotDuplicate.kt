// WITH_STDLIB
// CHECK_BYTECODE_TEXT
// ENHANCED_COROUTINES_DEBUGGING
// TARGET_BACKEND: JVM

import kotlin.coroutines.*

suspend fun foo(i: Int) {
    println("Start foo")
    coroutineScope {
        if (i == 25) {
            startMethod(i)
        }
        delay(1)
        println("After delay $i")
    }
    println("coroutineScope completed $i")
}

suspend fun startMethod(i: Int) {}

suspend fun coroutineScope(c: suspend () -> Unit) {}

suspend fun delay(i: Int) {}

fun builder(c: suspend () -> Unit) {
    c.startCoroutine(Continuation(EmptyCoroutineContext) {
        it.getOrThrow()
    })
}

fun box(): String {
    builder {
        foo(1)
    }
    return "OK"
}

// 1 LINENUMBER 14 L*
