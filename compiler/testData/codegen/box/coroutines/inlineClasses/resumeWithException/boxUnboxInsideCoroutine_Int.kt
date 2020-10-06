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
inline class IC(val s: Int)

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
        return foo(qux(quz(suspendMe())))
    }

    suspend fun test() = bar().s
}


class Test2 {

    suspend fun foo(value: IC): IC = value

    suspend fun qux(s: Int): IC = IC(s)

    suspend fun quz(): Int = suspendMe()

    suspend fun bar(): IC {
        return foo(qux(quz()))
    }

    suspend fun test() = bar().s
}

class Test3 {
    suspend fun <T> foo(value: T): T = value

    suspend fun bar(): IC {
        return foo(suspendMe())
    }

    suspend fun test() = bar().s
}

fun Int.toBoxResult() =
    if (this == 42) "OK" else toString()

fun box(): String {
    builder {
        Test1().test().toBoxResult()
    }
    c?.resumeWithException(IllegalStateException("OK"))

    if (result != "OK") return "FAIL 1 $result"

    result = "FAIL2"

    builder {
        Test2().test().toBoxResult()
    }
    c?.resumeWithException(IllegalStateException("OK"))

    if (result != "OK") return "FAIL 2 $result"

    result = "FAIL 3"

    builder {
        Test3().test().toBoxResult()
    }
    c?.resumeWithException(IllegalStateException("OK"))

    return result as String
}
