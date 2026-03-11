// WITH_STDLIB
// TARGET_BACKEND: JVM

// MODULE: lib
// FILE: 1.kt
package test

class C(val x: String) {
    companion object {
        @JvmField
        val instance: C = C("OK")
    }
}

// MODULE: main(lib)
// FILE: 2.kt
import test.C.Companion.instance

fun box() = instance.x
