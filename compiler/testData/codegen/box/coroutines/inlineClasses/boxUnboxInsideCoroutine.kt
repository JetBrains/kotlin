// IGNORE_BACKEND_FIR: JVM_IR
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
        return foo(qux(quz(IC("OK"))))
    }

    suspend fun test() = bar().s
}


class Test2 {

    suspend fun foo(value: IC): IC {
        return suspendCoroutineUninterceptedOrReturn {
            it.resume(value)
            COROUTINE_SUSPENDED
        }
    }

    suspend fun qux(s: String): IC = IC(s)

    suspend fun quz(): String = "OK"

    suspend fun bar(): IC {
        return foo(qux(quz()))
    }

    suspend fun test() = bar().s
}

class Test3 {
    suspend fun <T> foo(value: T): T {
        return suspendCoroutineUninterceptedOrReturn {
            it.resume(value)
            COROUTINE_SUSPENDED
        }
    }

    suspend fun bar(): IC {
        return foo(IC("OK"))
    }

    suspend fun test() = bar().s
}

fun box(): String {

    var result = "FAIL"
    builder {
        result = Test1().test()
    }

    if (result != "OK") return "FAIL 1 $result"

    result = "FAIL2"

    builder {
        result = Test2().test()
    }

    if (result != "OK") return "FAIL 2 $result"

    result = "FAIL 3"

    builder {
        result = Test3().test()
    }

    return result
}
