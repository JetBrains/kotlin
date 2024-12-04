// TARGET_BACKEND: JVM_IR
// FILE: 1.kt
package test

class C {
    var x: () -> Unit
        inline get() = {}
        inline set(crossinline value) {
            bar { value() }
        }

    fun bar(i: I) = i.foo()
}

fun interface I {
    fun foo()
}

// FILE: 2.kt
import test.*

fun box(): String {
    var result = "fail"
    val c = C()
    c.x = { result = "OK" }
    return result
}
