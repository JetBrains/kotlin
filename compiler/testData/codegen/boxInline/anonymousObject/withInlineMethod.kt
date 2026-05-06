// FILE: 1.kt
package test

interface I {
    fun f(): String
}

inline fun test(crossinline h: () -> String) = object : I {
    override fun f(): String = g()
    inline fun g(): String = h()
}

// FILE: 2.kt

import test.*

fun box(): String = test { "OK" }.f()
