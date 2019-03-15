// IGNORE_BACKEND: JVM_IR
// FILE: 1.kt

package test

private class S {
    fun a(): String {
        return "K"
    }
}

// This function exposes S which is a private class (package-private in the byte code)
// It can be accessed outside the `test` package, which may lead to IllegalAccessError.
// This behavior may be changed later
internal inline fun call(s: () -> String): String {
    val s = test()
    return s() + test2(s)
}

private fun test(): S {
    return S()
}

private fun test2(s: S): String {
    return s.a()
}

// FILE: 2.kt

import test.*

fun box(): String {
    return call {
        "O"
    }
}
