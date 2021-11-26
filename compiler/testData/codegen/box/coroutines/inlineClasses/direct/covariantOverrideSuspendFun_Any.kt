// WITH_STDLIB
// WITH_COROUTINES

import helpers.*
import kotlin.coroutines.*

fun builder(c: suspend () -> Unit) {
    c.startCoroutine(EmptyContinuation)
}

@Suppress("UNSUPPORTED_FEATURE")
inline class IC(val s: Any)

interface IBar {
    suspend fun bar(): Any
}

class Test() : IBar {

    override suspend fun bar(): IC = IC("OK")

    suspend fun test1(): String {
        val b: IBar = this
        return (b.bar() as IC).s as String
    }

    suspend fun test2(): String = bar().s as String
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