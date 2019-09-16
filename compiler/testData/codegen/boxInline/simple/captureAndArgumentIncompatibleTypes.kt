// FILE: 1.kt
package test

inline fun foo(i: Int) = i.toFloat()

// FILE: 2.kt
import test.*

fun box(): String {
    val captured = 1.0f
    val result = 1.let { captured + foo(it) }
    return if (result == 2.0f) "OK" else "Fail: $result"
}
