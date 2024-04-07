// ISSUE: KT-66046

open class A1 {
    var a: Int = 10
        protected set
}

interface I1 {
    var a: Int
}

interface I12 {
    <!WRONG_MODIFIER_CONTAINING_DECLARATION!>internal<!> var a: Int
}

abstract class <!CANNOT_WEAKEN_ACCESS_PRIVILEGE_WARNING!>B1<!> : A1(), I1
abstract class <!CANNOT_CHANGE_ACCESS_PRIVILEGE_WARNING!>B12<!> : A1(), I12

open class A2 {
    protected fun foo(): Int = 10
}

interface I2 {
    fun foo(): Int
}

abstract class <!CANNOT_WEAKEN_ACCESS_PRIVILEGE!>B2<!> : A2(), I2

interface I3 {
    <!WRONG_MODIFIER_CONTAINING_DECLARATION!>internal<!> var bar: String
}

interface I4 {
    var bar: String
    <!WRONG_MODIFIER_CONTAINING_DECLARATION!>protected<!> set
}

abstract <!CANNOT_INFER_VISIBILITY_WARNING!>class B3<!> : I3, I4
