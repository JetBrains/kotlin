// WITH_RUNTIME
// WITH_COROUTINES
// CHECK_BYTECODE_LISTING
// TARGET_BACKEND: JVM
// IGNORE_BACKEND: JVM

import helpers.*
import kotlin.coroutines.*

fun builder(c: suspend () -> Unit) {
    c.startCoroutine(EmptyContinuation)
}

suspend fun call(c: suspend Long.() -> String): String {
    return 1000L.c()
}

fun box(): String {
    var res = ""
    builder {
        res = call { ->
            "OK$this"
        }
    }
    if (res != "OK1000") return res
    return "OK"
}