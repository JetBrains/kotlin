// FILE: 1.kt

package test

class Foo(val a: String) {

    fun test() = a
}

inline fun test(a: String, b: () -> String, c: () -> String, d: String): String {
    return a + b() + c() + d
}

// FILE: 2.kt

import test.*

var effects = ""

fun create(a: String): Foo {
    effects += a
    return Foo(a)
}

fun box(): String {
    val result = test(create("A").a, create("B")::a, create("C")::test, create("D").a)
    if (result != effects) return "fail 1: $effects != $result"

    return if (result == "ABCD") "OK" else "fail 2: $result"
}