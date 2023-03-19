// TARGET_BACKEND: JVM_IR
// IGNORE_BACKEND_MULTI_MODULE: JVM_MULTI_MODULE_IR_AGAINST_OLD
// FILE: 1.kt
package test

class C {
    var x: () -> Unit
        inline get() = {}
        inline set(noinline value) {
            bar(value)
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
