// WITH_STDLIB
// WITH_COROUTINES
// IGNORE_BACKEND: JVM

import kotlin.coroutines.startCoroutine
import helpers.EmptyContinuation

suspend fun test() {
    suspend fun process(myValue: UInt) {
        if (myValue != 42u) throw AssertionError(myValue)
    }
    val value: UInt = 42u
    process(value)
}

fun builder(block: suspend () -> Unit) {
    block.startCoroutine(EmptyContinuation)
}

fun box(): String {
    builder { test() }
    return "OK"
}
