// FILE: 1.kt
package test

inline fun f() {}

// FILE: 2.kt
import test.*

inline fun g() {}

inline fun h() {
    f() // line N+1 -> 1.kt:4
    g() // line N+2 -> 2.kt:4
    f() // line N+3 -> 1.kt:4 again
}

fun box(): String {
    h()
    return "OK"
}

