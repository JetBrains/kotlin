// WITH_RUNTIME
// TARGET_BACKEND: JVM
// FILE: 1.kt
package test

class C(val x: String) {
    companion object {
        @JvmField
        val instance: C = C("OK")
    }
}

// FILE: 2.kt
import test.C.Companion.instance

fun box() = instance.x
