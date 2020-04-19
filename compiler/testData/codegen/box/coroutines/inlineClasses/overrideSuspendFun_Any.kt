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

class Test1() : IBar {

    suspend fun <T> foo(value: T): T {
        return suspendCoroutineUninterceptedOrReturn {
            it.resume(value)
            COROUTINE_SUSPENDED
        }
    }

    suspend fun qux(ss: IC): IC = IC(ss.s)

    suspend fun <T> quz(t: T): T = t

    override suspend fun bar(): IC {
        return foo(qux(quz(IC("OK"))))
    }

    suspend fun test(): Any {
        val b: IBar = this
        return b.bar().s
    }
}


class Test2 : IBar {

    suspend fun foo(value: IC): IC {
        return suspendCoroutineUninterceptedOrReturn {
            it.resume(value)
            COROUTINE_SUSPENDED
        }
    }

    suspend fun qux(s: String): IC = IC(s)

    suspend fun quz(): String = "OK"

    override suspend fun bar(): IC {
        return foo(qux(quz()))
    }

    suspend fun test(): Any {
        val b: IBar = this
        return b.bar().s
    }
}

class Test3 : IBar {
    suspend fun <T> foo(value: T): T {
        return suspendCoroutineUninterceptedOrReturn {
            it.resume(value)
            COROUTINE_SUSPENDED
        }
    }

    override suspend fun bar(): IC {
        return foo(IC("OK"))
    }

    suspend fun test(): Any {
        val b: IBar = this
        return b.bar().s
    }
}

fun box(): String {
    var result: Any = "FAIL1"
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
    return result as String
}
