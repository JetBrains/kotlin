// TODO: muted automatically, investigate should it be ran for JS or not
// IGNORE_BACKEND: JS
// COMMON_COROUTINES_TEST
// WITH_RUNTIME
// WITH_COROUTINES

import helpers.*
import kotlin.reflect.*
import COROUTINES_PACKAGE.*

fun builder(c: suspend () -> Unit) {
    c.startCoroutine(EmptyContinuation)
}

class A {
    val x = 1
    suspend fun x(): String = "OK"
}

val f1: KProperty1<A, Int> = A::x
val f2: suspend (A) -> String = A::x

fun box(): String {
    val a = A()

    val x1 = f1.get(a)
    if (x1 != 1) return "Fail 1: $x1"

    var res = "FAIL"
    builder {
        res = f2(a)
    }
    return res
}
