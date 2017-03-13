// FILE: 1.kt

package test

object Foo {
    val a: String = "OK"
}

inline fun test(s: () -> String): String {
    return s()
}

// FILE: 2.kt

import test.Foo.a
import test.test

fun box(): String {
    return test(::a)
}