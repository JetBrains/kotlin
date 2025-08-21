// TARGET_BACKEND: JVM
// WITH_STDLIB
// MODULE: library
// JVM_DEFAULT_MODE: disable
// FILE: a.kt
package base

interface A {
    fun Int.f(): Int = throw AssertionError()

    var Int.p: Int
        get() = throw AssertionError()
        set(value) { throw AssertionError() }
}

open class B : A

// MODULE: main(library)
// JVM_DEFAULT_MODE: enable
// FILE: source.kt
import base.*

interface C : A {
    override fun Int.f(): Int = this

    override var Int.p: Int
        get() = this
        set(value) {}
}

class D : B(), C

fun box(): String {
    with(D()) {
        if (1.f() != 1) return "Fail: ${1.f()}"
        2.p = 2
        if (3.p != 3) return "Fail: ${3.p}"
    }
    return "OK"
}
