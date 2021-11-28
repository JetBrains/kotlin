// IGNORE_BACKEND: JVM
// WITH_STDLIB
// WITH_COROUTINES
// KT-30041

import helpers.*
import kotlin.coroutines.*

var result = 0

fun builder(block: suspend () -> Unit) {
    block.startCoroutine(EmptyContinuation)
}

suspend fun test() {
    suspend fun local() {
        builder {
            if (result++ < 1) {
                local()
            }
        }
    }

    local()
}

fun box(): String {
    builder {
        test()
    }
    return if (result == 2) "OK" else "Fail: $result"
}
