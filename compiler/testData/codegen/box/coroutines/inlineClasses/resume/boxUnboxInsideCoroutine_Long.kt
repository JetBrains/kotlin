// WITH_RUNTIME
// WITH_COROUTINES
import helpers.*
import kotlin.coroutines.*
import kotlin.coroutines.intrinsics.*

fun builder(c: suspend () -> Unit) {
    c.startCoroutine(EmptyContinuation)
}

@Suppress("UNSUPPORTED_FEATURE")
inline class IC(val s: Long)

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

    suspend fun qux(s: Long): IC = IC(s)

    suspend fun quz() = suspendMe<Long>()

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

fun Long.toBoxResult() =
    if (this == 42L) "OK" else toString()

fun box(): String {

    var result: String = "FAIL"
    builder {
        result = Test1().test().toBoxResult()
    }
    c?.resume(IC(42L))

    if (result != "OK") return "FAIL 1 $result"

    result = "FAIL2"

    builder {
        result = Test2().test().toBoxResult()
    }
    c?.resume(42L)

    if (result != "OK") return "FAIL 2 $result"

    result = "FAIL 3"

    builder {
        result = Test3().test().toBoxResult()
    }
    c?.resume(IC(42L))

    return result as String
}
