// WITH_STDLIB
// WITH_COROUTINES
import helpers.*
import kotlin.coroutines.*
import kotlin.coroutines.intrinsics.*

fun builder(c: suspend () -> Unit) {
    c.startCoroutine(EmptyContinuation)
}

@Suppress("UNSUPPORTED_FEATURE")
inline class IC(val s: String)

var c: Continuation<Any>? = null

suspend fun <T> suspendMe(): T = suspendCoroutine {
    @Suppress("UNCHECKED_CAST")
    c = it as Continuation<Any>
}

fun suspendFunId(block: suspend () -> IC) = block

class Test1() {

    suspend fun <T> foo(value: T): T = value

    suspend fun qux(ss: IC): IC = IC(ss.s)

    suspend fun <T> quz(t: T): T = t

    suspend fun bar(): IC {
        return suspendFunId { foo(qux(quz(suspendMe()))) }()
    }

    suspend fun test() = bar().s
}

fun box(): String {

    var result = "FAIL"
    builder {
        result = Test1().test()
    }
    c?.resume(IC("OK"))

    if (result != "OK") return "FAIL 1 $result"

    return result
}
