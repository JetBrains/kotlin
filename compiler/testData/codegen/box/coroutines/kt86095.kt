// WITH_STDLIB
// WITH_COROUTINES

import helpers.*
import kotlin.coroutines.*

abstract class Runner {
    internal abstract suspend fun run(): String
}

suspend fun foo() {}

inline fun makeRunner(crossinline f: () -> String) =
    object : Runner() {
        override suspend fun run(): String {
            foo()
            return f()
        }
    }

fun builder(c: suspend () -> Unit) {
    c.startCoroutine(EmptyContinuation)
}

fun box(): String {
    var result = "fail"

    builder {
        result = makeRunner { "OK" }.run()
    }

    return result
}
