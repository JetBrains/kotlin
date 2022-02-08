// WITH_STDLIB
// WITH_COROUTINES
import helpers.*
import kotlin.coroutines.*
import kotlin.coroutines.intrinsics.*

var result = "FAIL"

fun builder(c: suspend () -> Unit) {
    c.startCoroutine(handleExceptionContinuation {
        result = it.message!!
    })
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
        val b: IBar = this
        return b.bar().s
    }
}

fun box(): String {
    builder {
        Test0().test()
    }
    c?.resumeWithException(IllegalStateException("OK"))
    if (result != "OK") return "FAIL 0 $result"

    return result as String
}
