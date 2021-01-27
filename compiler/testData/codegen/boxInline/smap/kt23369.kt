// FILE: 1+a.kt

package test

inline fun inlineFun(lambda: () -> String): String {
    return lambda()
}

// FILE: 2.kt
import test.*

fun box(): String {
    return inlineFun { "OK" }
}
