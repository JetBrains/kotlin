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

class Test() : IBar {
    override suspend fun bar(): IC = IC(Wrapper("OK"))

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
    if (result != "OK") return "FAIL 1 $result"

    result = "FAIL2 "
    builder {
        result = Test().test2()
    }
    if (result != "OK") return "FAIL 2 $result"

    return result
}
