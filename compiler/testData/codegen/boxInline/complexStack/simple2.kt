// FILE: 1.kt

package test

inline fun foo(x: String) = x

class A {
    fun test(s: String) = s
}

inline fun processRecords(block: (String) -> String): String {
    return A().test(block(foo("K")))
}

// FILE: 2.kt

import test.*

fun box(): String {
    return processRecords { "O" + it }
}
