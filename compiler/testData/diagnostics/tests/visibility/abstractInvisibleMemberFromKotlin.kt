// !LANGUAGE: +ProhibitInvisibleAbstractMethodsInSuperclasses
// MODULE: base
// FILE: Base.kt
package base

abstract class Base {
    fun foo(): String {
        return internalFoo()
    }
    internal abstract fun internalFoo(): String
}

open class BaseWithOverride : Base() {
    override fun internalFoo(): String = ""
}

// MODULE: intermediate(base)
// FILE: Intermediate.kt
package intermediate
import base.*

abstract class Intermediate : Base()

// MODULE: impl(base, intermediate)
// FILE: Impl.kt
package impl
import base.*
import intermediate.*

<!INVISIBLE_ABSTRACT_MEMBER_FROM_SUPER!>class ImplDirectFromBase<!> : Base()

<!INVISIBLE_ABSTRACT_MEMBER_FROM_SUPER!>object ImplObjDirectFromBase<!> : Base()

class ImplDirectFromBaseWithOverride : BaseWithOverride()

class ImplDirectFromBaseWithOverrid : Base() {
    <!CANNOT_OVERRIDE_INVISIBLE_MEMBER!>override<!> fun internalFoo(): String = ""
}

<!INVISIBLE_ABSTRACT_MEMBER_FROM_SUPER!>class ImplViaIntermediate<!> : Intermediate()

fun foo() {
    ImplDirectFromBase().foo()
    ImplObjDirectFromBase.foo()
    ImplDirectFromBaseWithOverride().foo()
    ImplViaIntermediate().foo()
}
