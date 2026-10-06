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
inline class IC(val s: Any?)

var c: Continuation<Any>? = null

suspend fun <T> suspendMe(): T = suspendCoroutine {
    @Suppress("UNCHECKED_CAST")
    c = it as Continuation<Any>
}

interface IBar {
    suspend fun bar(): IC?
}

class Test1() : IBar {
    override suspend fun bar(): IC = suspendMe()

    suspend fun test(): Any? {
        val b: IBar = this
        return b.bar()!!.s
    }
}

class Test2() : IBar {
    override suspend fun bar(): IC = suspendMe()

    suspend fun test(): IC {
        val b: IBar = this
        return b.bar()!!
    }
}


fun box(): String {
    builder {
        Test1().test()
    }
    c?.resumeWithException(IllegalStateException("OK"))
    if (result != "OK") return "FAIL 1 $result"

    result = "FAIL 2"
    builder {
        Test2().test()
    }
    c?.resumeWithException(IllegalStateException("OK"))
    if (result != "OK") return "FAIL 2 $result"

    return "OK"
}
