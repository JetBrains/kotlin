// MODULE: library
// JVM_DEFAULT_MODE: disable
// FILE: a.kt
package base

interface A {
    fun f() {}
}

open class B : A

// MODULE: main(library)
// JVM_DEFAULT_MODE: all-compatibility
// FILE: source.kt
import base.*

interface C : A {
    override fun f() {}
}

abstract class D : B() {
    abstract override fun f()
}

abstract class E : D(), C
