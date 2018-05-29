// IGNORE_BACKEND: JS

// COMMON_COROUTINES_TEST
// WITH_RUNTIME
// WITH_COROUTINES

import helpers.*
import COROUTINES_PACKAGE.*

fun builder(c: suspend () -> Unit) {
    c.startCoroutine(EmptyContinuation)
}

suspend fun Boolean.foo() = 1
suspend fun Byte.foo() = 2
suspend fun Short.foo() = 3
suspend fun Int.foo() = 4
suspend fun Long.foo() = 5
suspend fun Char.foo() = 6
suspend fun Float.foo() = 7
suspend fun Double.foo() = 8

fun testRef(name: String, f: suspend () -> Int, expected: Int) {
    builder {
        val actual = f()
        if (actual != expected) throw AssertionError("$name: $actual != $expected")
    }
}

fun box(): String {
    testRef("Boolean", true::foo, 1)
    testRef("Byte", 1.toByte()::foo, 2)
    testRef("Short", 1.toShort()::foo, 3)
    testRef("Int", 1::foo, 4)
    testRef("Long", 1L::foo, 5)
    testRef("Char", '1'::foo, 6)
    testRef("Float", 1.0F::foo, 7)
    testRef("Double", 1.0::foo, 8)

    return "OK"
}