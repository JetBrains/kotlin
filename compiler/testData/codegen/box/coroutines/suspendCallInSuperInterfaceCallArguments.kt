// WITH_STDLIB
// WITH_COROUTINES
// TARGET_BACKEND: JVM

import helpers.*
import kotlin.coroutines.*

interface IFoo {
    fun foo(a: String): String =
        bar() + a

    fun bar(): String
}

suspend fun suspendK(a: String) =
    a + "K"

class FooImpl : IFoo {
    suspend fun test(a: String): String =
        super<IFoo>.foo(suspendK(a))

    override fun bar(): String = "O"
}

fun builder(c: suspend () -> Unit) {
    c.startCoroutine(EmptyContinuation)
}

fun box(): String {
    var result = "FAIL"
    builder {
        result = FooImpl().test("")
    }
    return result
}
