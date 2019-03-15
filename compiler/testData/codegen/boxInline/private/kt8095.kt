// IGNORE_BACKEND: JVM_IR
// FILE: 1.kt

package test

class C(private val a : String) {
    internal inline fun g(x: (s: String) -> Unit) {
        x(a)
    }
}

// FILE: 2.kt

import test.*

fun box(): String {
    var r = "fail"
    C("OK").g { r = it }
    return r
}
