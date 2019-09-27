// FILE: 1.kt
package test

inline fun f(g: () -> Int) {}
inline fun h(g: () -> Int) = run { f(g) }

// FILE: 2.kt
import test.*

fun box(): String {
    h { 1 }
    return "OK"
}
