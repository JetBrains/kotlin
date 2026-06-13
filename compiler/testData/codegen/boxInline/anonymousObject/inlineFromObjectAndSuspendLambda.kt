// WITH_COROUTINES
// WITH_STDLIB

// FILE: 1.kt
package test

suspend inline fun test(crossinline x: suspend () -> Unit): Unit = object {
    inline suspend fun f(): Unit = x()
}.f()

// FILE: 2.kt

import test.*
import kotlin.coroutines.*
import helpers.*

fun builder(c: suspend () -> Unit) {
    c.startCoroutine(EmptyContinuation)
}

var result = "Fail"

fun box(): String {
    builder {
        test {
            result = "OK"
        }
    }
    return result
}
