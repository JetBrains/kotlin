// FILE: 1.kt
package test

interface I {
    fun f(): String
}

inline fun test(crossinline h: () -> String) = object : I {
    // TODO: actually call g() in f() -- currently, the inliner fails to detect
    //       an inlined read of h's field because it uses a copy of `this`
    //       as a receiver
    override fun f(): String = h()
    inline fun g(): String = h()
}

// FILE: 2.kt

import test.*

fun box(): String = test { "OK" }.f()
