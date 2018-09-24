// IGNORE_BACKEND: JVM_IR
// FILE: 1.kt

package test

inline fun <reified T> test(x: Any): Boolean {
    val x = object {
        val y = x is T
    }

    return x.y
}

// FILE: 2.kt

import test.*

fun box(): String {
    if (!test<String>("OK")) return "fail 1"

    if (test<Int>("OK")) return "fail 2"

    return "OK"
}
