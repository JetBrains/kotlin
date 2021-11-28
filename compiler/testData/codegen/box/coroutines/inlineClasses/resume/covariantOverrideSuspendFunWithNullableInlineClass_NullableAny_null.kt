// WITH_STDLIB
// WITH_COROUTINES

import helpers.*
import kotlin.coroutines.*

fun builder(c: suspend () -> Unit) {
    c.startCoroutine(EmptyContinuation)
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
    var result: Any? = "FAIL 1"
    builder {
        result = Test1().test()
    }
    c?.resume(IC(null))
    if (result != null) return "FAIL 1 $result"

    result = "FAIL 2"
    builder {
        result = Test2().test()
    }
    c?.resume(IC(null))
    if (result != IC(null)) return "FAIL 2 $result"

    return "OK"
}
