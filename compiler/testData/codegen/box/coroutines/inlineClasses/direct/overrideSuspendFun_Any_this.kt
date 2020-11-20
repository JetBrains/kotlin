// WITH_RUNTIME
// WITH_COROUTINES
// COMMON_COROUTINES_TEST

import helpers.*
import COROUTINES_PACKAGE.*
import COROUTINES_PACKAGE.intrinsics.*

fun builder(c: suspend () -> Unit) {
    c.startCoroutine(EmptyContinuation)
}

@Suppress("UNSUPPORTED_FEATURE")
inline class IC(val s: Any)

interface IBar {
    suspend fun bar(): IC
}

class Test0() : IBar {
    override suspend fun bar(): IC = IC("OK")

    suspend fun test(): Any {
        return bar().s
    }
}

fun box(): String {
    var result: Any = "FAIL"
    builder {
        result = Test0().test()
    }
    if (result != "OK") return "FAIL 0 $result"

    return result as String
}
