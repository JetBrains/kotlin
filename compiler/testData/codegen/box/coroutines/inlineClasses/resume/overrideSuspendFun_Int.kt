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
inline class IC(val s: Int)

var c: Continuation<Any>? = null

suspend fun <T> suspendMe(): T = suspendCoroutine {
    @Suppress("UNCHECKED_CAST")
    c = it as Continuation<Any>
}

interface IBar {
    suspend fun bar(): IC
}

class Test1() : IBar {

    suspend fun <T> foo(value: T): T = value

    suspend fun qux(ss: IC): IC = IC(ss.s)

    suspend fun <T> quz(t: T): T = t

    override suspend fun bar(): IC {
        return foo(qux(quz(suspendMe())))
    }

    suspend fun test(): Int {
        val b: IBar = this
        return b.bar().s
    }
}


class Test2 : IBar {

    suspend fun foo(value: IC): IC = value

    suspend fun qux(s: Int): IC = IC(s)

    suspend fun quz(): Int = suspendMe()

    override suspend fun bar(): IC {
        return foo(qux(quz()))
    }

    suspend fun test(): Int {
        val b: IBar = this
        return b.bar().s
    }
}

class Test3 : IBar {
    suspend fun <T> foo(value: T): T = value

    override suspend fun bar(): IC {
        return foo(suspendMe())
    }

    suspend fun test(): Int {
        val b: IBar = this
        return b.bar().s
    }
}

fun Int.toBoxResult() =
    if (this == 42) "OK" else toString()

fun box(): String {

    var result: String = "FAIL"
    builder {
        result = Test1().test().toBoxResult()
    }
    c?.resume(IC(42))

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
    c?.resume(IC(42))

    return result as String
}
