// WITH_STDLIB
// WITH_COROUTINES
import helpers.*
import kotlin.coroutines.*
import kotlin.coroutines.intrinsics.*

fun builder(c: suspend () -> Unit) {
    c.startCoroutine(EmptyContinuation)
}

@Suppress("UNSUPPORTED_FEATURE")
inline class IC(val s: Any)

var c: Continuation<Any>? = null

suspend fun <T> suspendMe(): T = suspendCoroutine {
    @Suppress("UNCHECKED_CAST")
    c = it as Continuation<Any>
}

interface IBar {
    suspend fun bar(): IC
}

class Test0() : IBar {
    override suspend fun bar(): IC = suspendMe()

    suspend fun test(): Any {
        return bar().s
    }
}

fun box(): String {
    var result: Any = "FAIL"
    builder {
        result = Test0().test()
    }
    c?.resume(IC("OK"))
    if (result != "OK") return "FAIL 0 $result"

    return result as String
}
