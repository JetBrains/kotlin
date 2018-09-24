// IGNORE_BACKEND: JVM_IR
// WITH_RUNTIME
// WITH_COROUTINES
// COMMON_COROUTINES_TEST
import helpers.*
import COROUTINES_PACKAGE.*
import COROUTINES_PACKAGE.intrinsics.*

class TestClass {
    suspend fun toInt(): Int = suspendCoroutineUninterceptedOrReturn { x ->
        x.resume(14)
        COROUTINE_SUSPENDED
    }
}

fun builder(c: suspend () -> Unit) {
    c.startCoroutine(EmptyContinuation)
}

fun box(): String {
    var result = 0

    var instance: TestClass? = null

    builder {
        result = 42 + (instance?.toInt() ?: 0)
    }

    if (result != 42) return "fail 1: $result"

    instance = TestClass()
    builder {
        result = 42 + (instance?.toInt() ?: 0)
    }

    if (result != 56) return "fail 2: $result"

    return "OK"
}
