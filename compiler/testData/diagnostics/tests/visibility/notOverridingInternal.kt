// FIR_IDENTICAL
// MODULE: base
// FILE: Base.kt
package base

abstract class Base {
    fun foo() = internalFoo()

    internal fun internalFoo() {}
}

// MODULE: impl(base)
// FILE: Impl.kt
package impl
import base.*

class Impl : Base() {
    fun internalFoo() { /*not an override*/ }
}

fun foo() {
    Impl().foo()
    Impl().internalFoo()
}
