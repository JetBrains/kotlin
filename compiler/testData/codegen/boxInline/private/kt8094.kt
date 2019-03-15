// IGNORE_BACKEND: JVM_IR
// FILE: 1.kt

package test

object X {
    private fun f() { }

    internal inline fun g(x: () -> Unit) {
        x()
        f()
    }
}

// FILE: 2.kt

import test.*

fun box(): String {
    var r = "fail"
    X.g { r = "OK" }

    return r;
}
