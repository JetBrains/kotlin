// ISSUE: KT-85605

// FILE: test.kt
package test

@Suppress("NOTHING_TO_INLINE")
inline fun test() {
    run { val x: Any? by lazy { null } }
}

// FILE: box.kt
import test.*

fun box(): String {
    run { test() }
    return "OK"
}
