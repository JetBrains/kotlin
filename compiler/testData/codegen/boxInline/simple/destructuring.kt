// FILE: 1.kt
package test

inline fun foo(x: (Int, Station) -> Unit) {
    x(1, Station(null, "", 1))
}

data class Station(
        val id: String?,
        val name: String,
        val distance: Int)


// FILE: 2.kt
import test.*

fun box(): String {
    foo { i, (a1, a2, a3) -> i + a3 }
    return "OK"
}