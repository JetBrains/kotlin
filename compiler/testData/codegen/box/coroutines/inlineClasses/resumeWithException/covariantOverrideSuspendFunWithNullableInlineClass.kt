// WITH_STDLIB
// WITH_COROUTINES

import helpers.*
import kotlin.coroutines.*

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

interface IBar {
    suspend fun bar(): IC?
}

class Test() : IBar {

    override suspend fun bar(): IC = suspendMe()

    suspend fun test1(): String {
        val b: IBar = this
        return b.bar()!!.s
    }

    suspend fun test2(): String {
        return bar().s
    }
}

fun box(): String {
    builder {
        Test().test1()
    }
    c?.resumeWithException(IllegalStateException("OK"))
    if (result != "OK") return "FAIL 1 $result"

    result = "FAIL2"
    builder {
        Test().test2()
    }
    c?.resumeWithException(IllegalStateException("OK"))
    if (result != "OK") return "FAIL 2 $result"

    return result
}
