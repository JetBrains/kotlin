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

abstract class B1 : A1(), I1
abstract class B12 : A1(), I12

open class A2 {
    protected fun foo(): Int = 10
}

interface I2 {
    fun foo(): Int
}

abstract class <!CANNOT_INFER_VISIBILITY!>B2<!> : A2(), I2

interface I3 {
    <!WRONG_MODIFIER_CONTAINING_DECLARATION!>internal<!> var bar: String
}

interface I4 {
    var bar: String
    <!WRONG_MODIFIER_CONTAINING_DECLARATION!>protected<!> set
}

abstract class B3 : I3, I4
