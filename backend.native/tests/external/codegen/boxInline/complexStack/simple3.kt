// FILE: 1.kt

package test

inline fun foo(x: String, y: String) = x + y

class A {
    fun test(s: String) = s
}

inline fun processRecords(block: (String) -> String): String {
    return A().test(block(foo("O", foo("K", "1"))))
}

// FILE: 2.kt

import test.*

fun box(): String {
    val result = processRecords { "B" + it }

    return if (result == "BOK1") "OK" else "fail: $result"
}
