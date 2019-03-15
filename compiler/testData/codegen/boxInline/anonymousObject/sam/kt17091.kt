// IGNORE_BACKEND: JVM_IR
// FILE: 1.kt
// FULL_JDK

package test

inline fun foo(value: String, crossinline s: () -> String): String {
    val x = { value }
    return java.util.concurrent.Callable(x).call() + { s() }()
}


// FILE: 2.kt

import test.*

fun box(): String {
    return foo("O") { "K" }
}
