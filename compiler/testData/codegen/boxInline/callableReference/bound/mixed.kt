// FILE: 1.kt

package test

class Foo {
    fun foo() = "OK"
    fun foo2() = "OK2"
}

inline fun inlineFn(a: String, crossinline fn: () -> String, x: Long = 1, crossinline fn2: () -> String, c: String): String {
    return a + fn() + x + fn2() + c
}

// FILE: 2.kt

import test.*

private val foo = Foo()

fun box(): String {
    val result = inlineFn("a", foo::foo, 5, foo::foo2, "end")
    return if (result == "aOK5OK2end") "OK" else "fail: $result"
}