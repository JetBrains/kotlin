// WITH_STDLIB
// WITH_COROUTINES
// KT-27830
// NO_CHECK_LAMBDA_INLINING

// FILE: main.kt
import helpers.EmptyContinuation
import kotlin.coroutines.*

fun box(): String {
    var result = "Fail"
    suspend {
        do {
            go {
                result = "OK"
            }
        } while (false)
    }.startCoroutine(EmptyContinuation)
    return result
}

// FILE: lib.kt
suspend inline fun go(block: suspend () -> Unit) {
    block()
}
