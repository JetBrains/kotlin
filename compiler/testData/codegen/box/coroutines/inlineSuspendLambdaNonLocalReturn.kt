// WITH_STDLIB
// WITH_COROUTINES
// FILE: lib.kt
import helpers.*
import kotlin.coroutines.*
import kotlin.coroutines.intrinsics.*

suspend inline fun doTwice(block: suspend () -> Unit) {
    block()
    block()
}

// FILE: main.kt
import helpers.*
import kotlin.coroutines.*
import kotlin.coroutines.intrinsics.*

var testResult: String = ""

fun build(c: suspend () -> Unit) {
    c.startCoroutine(EmptyContinuation)
}

fun box(): String {

    build {
        doTwice {
            testResult +=  "OK"
            return@build
        }
    }

    return testResult
}
