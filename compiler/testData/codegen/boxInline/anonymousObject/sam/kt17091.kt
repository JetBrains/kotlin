// FULL_JDK
// TARGET_BACKEND: JVM
// FILE: 1.kt

package test

inline fun foo(value: String, crossinline s: () -> String): String {
    val x = { value }
    return java.util.concurrent.Callable(x).call() + { s() }.let { it() }
}


// FILE: 2.kt

import test.*

fun box(): String {
    return foo("O") { "K" }
}
