// IGNORE_BACKEND: JVM
// WITH_STDLIB
// WITH_COROUTINES
// DONT_RUN_GENERATED_CODE: JS

import helpers.*
import kotlin.coroutines.*

var iterations = 0

fun builder(c: suspend () -> Unit) {
    c.startCoroutine(EmptyContinuation)
}

fun box(): String {
    var result = ""

    tailrec suspend fun theLoop(value: Int, string: String) {
        if (value == 2) return
        result += string
        if (++iterations > 2) error("Fail: too many iterations")
        theLoop(value + 1, "b")
    }

    builder {
        theLoop(0, "a")
    }

    return if (result == "ab") "OK" else "Fail $result"
}
