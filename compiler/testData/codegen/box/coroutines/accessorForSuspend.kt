// IGNORE_BACKEND: JVM_IR
// WITH_RUNTIME
// WITH_COROUTINES
// COMMON_COROUTINES_TEST
import helpers.*
import COROUTINES_PACKAGE.*
import COROUTINES_PACKAGE.intrinsics.*

fun builder(c: suspend () -> Unit) {
    c.startCoroutine(EmptyContinuation)
}

class A {
    suspend private fun a(): String = suspendCoroutineUninterceptedOrReturn { x ->
        x.resume("OK")
        COROUTINE_SUSPENDED
    }

    suspend fun myfun(): String {
        var result = "fail"
        builder {
            result = a()
        }

        return result
    }
}

fun box(): String {
    var result = ""

    builder {
        result = A().myfun()
    }

    return result
}
