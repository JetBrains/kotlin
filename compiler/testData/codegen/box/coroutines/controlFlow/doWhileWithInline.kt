// WITH_STDLIB
// WITH_COROUTINES
// KT-27830

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

suspend inline fun go(block: suspend () -> Unit) {
    block()
}
