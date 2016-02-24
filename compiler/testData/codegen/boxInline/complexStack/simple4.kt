// FILE: 1.kt

package test

inline fun foo(x: String) = x

fun test(a: String, s: String) = s


inline fun processRecords(block: (String, String) -> String): String {
    return test("stub", block(foo("O"), foo("K")))
}

// FILE: 2.kt

import test.*

fun box(): String {
    return processRecords { a, b -> a + b}
}
