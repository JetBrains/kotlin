// WITH_STDLIB
// WITH_COROUTINES

import helpers.*
import kotlin.coroutines.*

fun builder(c: suspend () -> Unit) {
    c.startCoroutine(EmptyContinuation)
}

@Suppress("UNSUPPORTED_FEATURE")
inline class IC(val s: Int)

var c: Continuation<Any>? = null

suspend fun <T> suspendMe(): T = suspendCoroutine {
    @Suppress("UNCHECKED_CAST")
    c = it as Continuation<Any>
}

interface IBar {
    suspend fun bar(): Any
}

class Test() : IBar {
    override suspend fun bar(): IC = suspendMe()

    suspend fun test1(): Int {
        val b: IBar = this
        return (b.bar() as IC).s
    }

    suspend fun test2(): Int {
        val b: IBar = this
        return (b.bar() as IC).s
    }
}

fun Int.toBoxResult() =
    if (this == 42) "OK" else toString()

fun box(): String {
    var result: String = "FAIL"
    builder {
        result = Test().test1().toBoxResult()
    }
    c?.resume(IC(42))
    if (result != "OK") return "FAIL 1 $result"

    result = "FAIL2"
    builder {
        result = Test().test2().toBoxResult()
    }
    c?.resume(IC(42))
    if (result != "OK") return "FAIL 2 $result"

    return result as String
}
