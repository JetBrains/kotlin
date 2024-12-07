// FILE: 1.kt
package test

class C(val x: String) {
    inline fun f(crossinline h: () -> String) = C("").g { x + h() }

    inline fun g(crossinline h: () -> String) =
        { { h() + x }.let { it() } }.let { it() }
}

// FILE: 2.kt
import test.*

fun box() = C("O").f { "K" }
