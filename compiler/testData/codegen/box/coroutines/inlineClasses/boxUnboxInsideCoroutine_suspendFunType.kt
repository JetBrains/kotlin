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
inline class IC(val s: String)

fun suspendFunId(block: suspend () -> IC) = block

class Test1() {

    suspend fun <T> foo(value: T): T {
        return suspendCoroutineUninterceptedOrReturn {
            it.resume(value)
            COROUTINE_SUSPENDED
        }
    }

    suspend fun qux(ss: IC): IC = IC(ss.s)

    suspend fun <T> quz(t: T): T = t

    suspend fun bar(): IC {
        return suspendFunId { foo(qux(quz(IC("OK")))) }()
    }

    suspend fun test() = bar().s
}

fun box(): String {

    var result = "FAIL"
    builder {
        result = Test1().test()
    }

    if (result != "OK") return "FAIL 1 $result"

    return result
}
