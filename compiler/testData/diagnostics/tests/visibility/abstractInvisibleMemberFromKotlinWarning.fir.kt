// !LANGUAGE: -ProhibitInvisibleAbstractMethodsInSuperclasses
// MODULE: base
// FILE: Base.kt
package base

abstract class Base {
    fun foo(): String {
        return internalFoo()
    }
    internal abstract fun internalFoo(): String
}

// MODULE: impl(base)
// FILE: Impl.kt
package impl
import base.*

class Impl : Base()

fun foo() {
    Impl().foo()
}
