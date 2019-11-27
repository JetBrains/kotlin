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

<!INVISIBLE_ABSTRACT_MEMBER_FROM_SUPER_WARNING!>class Impl<!> : Base()

fun foo() {
    Impl().foo()
}
