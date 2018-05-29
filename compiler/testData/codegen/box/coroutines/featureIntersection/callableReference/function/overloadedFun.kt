// IGNORE_BACKEND: JS

// COMMON_COROUTINES_TEST
// WITH_RUNTIME
// WITH_COROUTINES

import helpers.*
import COROUTINES_PACKAGE.*

fun builder(c: suspend () -> Unit) {
    c.startCoroutine(EmptyContinuation)
}

suspend fun foo(): String = "foo1"
suspend fun foo(i: Int): String = "foo2"

val f1: suspend () -> String = ::foo
val f2: suspend (Int) -> String = ::foo

suspend fun foo1() {}
suspend fun foo2(i: Int) {}

suspend fun bar(f: suspend () -> Unit): String = "bar1"
suspend fun bar(f: suspend (Int) -> Unit): String = "bar2"

fun box(): String {
    builder {
        val x1 = f1()
        if (x1 != "foo1") throw RuntimeException("Fail 1: $x1")

        val x2 = f2(0)
        if (x2 != "foo2") throw RuntimeException("Fail 2: $x2")

        val y1 = bar(::foo1)
        if (y1 != "bar1") throw RuntimeException("Fail 3: $y1")

        val y2 = bar(::foo2)
        if (y2 != "bar2") throw RuntimeException("Fail 4: $y2")
    }

    return "OK"
}
