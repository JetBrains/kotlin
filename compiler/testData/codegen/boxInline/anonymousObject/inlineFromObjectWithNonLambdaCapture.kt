// FILE: 1.kt
package test

interface I {
    fun f(): String
}

inline fun test(x: String) = object : I {
    override fun f(): String = g()
    inline fun g(): String = x
}

// FILE: 2.kt

import test.*

fun box(): String = test("OK").f()
