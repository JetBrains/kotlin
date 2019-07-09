// IGNORE_BACKEND: JVM_IR
// FILE: 1.kt

package test

inline fun f(
    wait: Int = 0,
    action: (Int) -> Unit
): String {
    var millis: Long = 1
    try {
    } catch (e: Throwable) {
        millis = millis
    }
    return "OK"
}

// FILE: 2.kt
import test.*

fun box(): String {
    var x = 0
    return f {
        x++
    }
}