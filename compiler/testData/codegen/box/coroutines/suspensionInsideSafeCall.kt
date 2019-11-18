// IGNORE_BACKEND_FIR: JVM_IR
// WITH_RUNTIME
// WITH_COROUTINES
// COMMON_COROUTINES_TEST
import helpers.*
import COROUTINES_PACKAGE.*
import COROUTINES_PACKAGE.intrinsics.*

class TestClass {
    suspend fun suspendHere(): String = suspendCoroutineUninterceptedOrReturn { x ->
        x.resume("K")
        COROUTINE_SUSPENDED
    }
}

fun builder(c: suspend () -> Unit) {
    c.startCoroutine(EmptyContinuation)
}

fun foo(x: String, y: String?) = x + (y ?: "")

fun box(): String {
    var result = ""

    var instance: TestClass? = null

    builder {
        result = foo("OK", instance?.suspendHere())
    }

    if (result != "OK") return "fail 1: $result"

    result = ""
    instance = TestClass()
    builder {
        result = foo("O", instance?.suspendHere())
    }

    if (result != "OK") return "fail 2: $result"

    return "OK"
}
