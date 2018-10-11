// IGNORE_BACKEND: JVM_IR
// FILE: 1.kt

package test

class A() {
    private val x = "OK"
    internal inline fun foo(p: (String) -> Unit) {
        p(x)
    }
}

// FILE: 2.kt

import test.*

fun box(): String {
    var r = "fail"
    A().foo { r = it }
    return r
}
