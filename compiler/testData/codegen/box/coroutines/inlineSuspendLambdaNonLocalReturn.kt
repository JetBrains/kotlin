// WITH_STDLIB
// WITH_COROUTINES
import helpers.*
import kotlin.coroutines.*
import kotlin.coroutines.intrinsics.*

suspend inline fun doTwice(block: suspend () -> Unit) {
    block()
    block()
}

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
