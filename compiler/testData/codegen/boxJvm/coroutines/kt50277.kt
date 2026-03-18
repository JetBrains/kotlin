// WITH_STDLIB
// WITH_COROUTINES
// TARGET_BACKEND: JVM

import helpers.*
import kotlin.coroutines.*

fun builder(c: suspend () -> Unit) {
    c.startCoroutine(EmptyContinuation)
}

suspend fun g(): Int = 42

suspend fun f(block: suspend (a: Int) -> Unit) {
    listOf(0).map { block(g()) }
}

fun box(): String {
    var result = "Failed"
    builder {
        f { result = "OK" }
    }
    return result
}
