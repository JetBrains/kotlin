// !LANGUAGE: +SuspendConversion
// WITH_STDLIB
// WITH_COROUTINES
// IGNORE_BACKEND: JVM

import helpers.*
import kotlin.coroutines.*
import kotlin.coroutines.intrinsics.*

fun builder(c: suspend () -> Unit) {
    c.startCoroutine(EmptyContinuation)
}

suspend fun runS(fn: suspend (String) -> String) = fn("O")

val lambda: (String) -> String = { it + "K" }

fun box(): String {
    var test = "Failed"
    builder {
        test = runS(lambda)
    }
    return test
}
