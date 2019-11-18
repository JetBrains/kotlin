// IGNORE_BACKEND_FIR: JVM_IR
// WITH_RUNTIME
// WITH_COROUTINES
// COMMON_COROUTINES_TEST
import helpers.*
import COROUTINES_PACKAGE.*
import COROUTINES_PACKAGE.intrinsics.*

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