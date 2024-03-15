// ISSUE: KT-66046

open class A1 {
    var a: Int = 10
        protected set
}

interface I1 {
    var a: Int
}

abstract class B1 : A1(), I1

open class A2 {
    protected fun foo(): Int = 10
}

interface I2 {
    fun foo(): Int
}

abstract class <!CANNOT_INFER_VISIBILITY!>B2<!> : A2(), I2
