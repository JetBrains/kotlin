// FILE: 1.kt
package test

inline fun test(x: String, y: String) = object {
    val copiedX: String = x
    inline fun f(): String = object {
        inline fun g(): String = h()
        inline fun h(): String = copiedX + y
    }.g()
}.f()

// FILE: 2.kt

import test.*

fun box(): String = test("O", "K")
