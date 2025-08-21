// TARGET_BACKEND: JVM
// MODULE: library
// JVM_DEFAULT_MODE: disable
// FILE: a.kt
package base

interface A {
    fun f(s: String = "OK"): String = "Fail"
}

open class B : A

// MODULE: main(library)
// JVM_DEFAULT_MODE: enable
// FILE: source.kt
import base.*

interface C : A {
    override fun f(s: String): String = s
}

class D : B(), C

fun box(): String = D().f()
