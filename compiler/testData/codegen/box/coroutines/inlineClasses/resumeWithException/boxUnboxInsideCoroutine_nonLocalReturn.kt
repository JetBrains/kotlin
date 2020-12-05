// WITH_RUNTIME
// WITH_COROUTINES
// COMMON_COROUTINES_TEST

import helpers.*
import COROUTINES_PACKAGE.*
import COROUTINES_PACKAGE.intrinsics.*

var result = "FAIL"

fun builder(c: suspend () -> Unit) {
    c.startCoroutine(handleExceptionContinuation {
        result = it.message!!
    })
}

@Suppress("UNSUPPORTED_FEATURE")
inline class IC(val s: String)

var c: Continuation<Any>? = null

suspend fun <T> suspendMe(): T = suspendCoroutine {
    @Suppress("UNCHECKED_CAST")
    c = it as Continuation<Any>
}

class Test1() {

    suspend fun <T> foo(value: T): T = value

    suspend fun qux(ss: IC): IC = IC(ss.s)

    suspend fun <T> quz(t: T): T = t

    suspend fun bar(): IC {
        run {
            return foo(qux(quz(suspendMe())))
        }
    }

    suspend fun test() = bar().s
}


class Test2 {

    suspend fun foo(value: IC): IC = value

    suspend fun qux(s: String): IC = IC(s)

    suspend fun quz(): String = suspendMe()

    suspend fun bar(): IC {
        run {
            return foo(qux(quz()))
        }
    }

    suspend fun test() = bar().s
}

class Test3 {
    suspend fun <T> foo(value: T): T = value

    suspend fun bar(): IC {
        run {
            return foo(suspendMe())
        }
    }

    suspend fun test() = bar().s
}

class Test4 {
    suspend fun <T> foo(value: T): T = value

    suspend fun bar(): IC? {
        run {
            return foo(suspendMe())
        }
    }

    suspend fun test() = bar()!!.s
}

fun box(): String {
    builder {
        Test1().test()
    }
    c?.resumeWithException(IllegalStateException("OK"))

    if (result != "OK") return "FAIL 1 $result"

    result = "FAIL2"

    builder {
        Test2().test()
    }
    c?.resumeWithException(IllegalStateException("OK"))

    if (result != "OK") return "FAIL 2 $result"

    result = "FAIL 3"

    builder {
        Test3().test()
    }
    c?.resumeWithException(IllegalStateException("OK"))

    if (result != "OK") return "FAIL 3 $result"

    builder {
        Test4().test()
    }
    c?.resumeWithException(IllegalStateException("OK"))

    return result
}
