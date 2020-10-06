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
inline class I0(val x: Int)

@Suppress("UNSUPPORTED_FEATURE")
inline class IC(val s: I0)

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
        return foo(qux(quz(IC(suspendMe()))))
    }

    suspend fun test() = bar().s.x
}


class Test2 {

    suspend fun foo(value: IC): IC = value

    suspend fun qux(s: Int): IC = IC(I0(s))

    suspend fun quz() = suspendMe<Int>()

    suspend fun bar(): IC {
        return foo(qux(quz()))
    }

    suspend fun test() = bar().s.x
}

class Test3 {
    suspend fun <T> foo(value: T): T = value

    suspend fun bar(): IC {
        return foo(IC(suspendMe()))
    }

    suspend fun test() = bar().s.x
}

fun Int.toBoxResult() =
    if (this == 42) "OK" else toString()

fun box(): String {

    var result: Any = "FAIL"
    builder {
        result = Test1().test().toBoxResult()
    }
    c?.resume(I0(42))

    if (result != "OK") return "FAIL 1 $result"

    result = "FAIL2"

    builder {
        result = Test2().test().toBoxResult()
    }
    c?.resume(42)

    if (result != "OK") return "FAIL 2 $result"

    result = "FAIL 3"

    builder {
        result = Test3().test().toBoxResult()
    }
    c?.resume(I0(42))

    return result as String
}
