// IGNORE_BACKEND_FIR: JVM_IR
// WITH_RUNTIME
// WITH_COROUTINES
// COMMON_COROUTINES_TEST
import helpers.*
import COROUTINES_PACKAGE.*
import COROUTINES_PACKAGE.intrinsics.*

interface TestInterface {
    suspend fun toInt(): Int = suspendCoroutineUninterceptedOrReturn { x ->
        x.resume(56)
        COROUTINE_SUSPENDED
    }
}

class TestClass2 : TestInterface {
}

fun builder(c: suspend () -> Unit) {
    c.startCoroutine(EmptyContinuation)
}

fun box(): String {
    var result = -1
    builder {
        result = TestClass2().toInt()
    }

    if (result != 56) return "fail 1: $result"

    return "OK"
}

