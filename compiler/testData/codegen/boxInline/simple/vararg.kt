// WITH_STDLIB
// KJS_WITH_FULL_RUNTIME
// FILE: 1.kt
package test

inline fun doSmth(vararg a: String) : String {
    return a.foldRight("", { a, b -> a + b})
}

// FILE: 2.kt

import test.*

fun test1(): String {
    return doSmth("O", "K")
}

fun box(): String {
    val result = test1()
    if (result != "OK") return "fail1: ${result}"

    return "OK"
}
