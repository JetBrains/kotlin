// WITH_STDLIB
// WITH_COROUTINES

import helpers.*
import kotlin.coroutines.*

fun builder(c: suspend () -> Unit) {
    c.startCoroutine(EmptyContinuation)
}

interface I

@Suppress("UNSUPPORTED_FEATURE")
inline class IC(val i: I): I

class Wrapper(val s: String): I

interface IBar {
    suspend fun bar(): I
}

var c: Continuation<Any>? = null

suspend fun <T> suspendMe(): T = suspendCoroutine {
    @Suppress("UNCHECKED_CAST")
    c = it as Continuation<Any>
}

class Test() : IBar {
    override suspend fun bar(): IC = suspendMe()

    suspend fun test1(): String {
        val b: IBar = this
        return ((b.bar() as IC).i as Wrapper).s
    }

    suspend fun test2(): String = ((bar() as IC).i as Wrapper).s
}

fun box(): String {
    var result = "FAIL 1"
    builder {
        result = Test().test1()
    }
    c?.resume(IC(Wrapper("OK")))
    if (result != "OK") return "FAIL 1 $result"

    result = "FAIL2 "
    builder {
        result = Test().test2()
    }
    c?.resume(IC(Wrapper("OK")))
    if (result != "OK") return "FAIL 2 $result"

    return result
}
