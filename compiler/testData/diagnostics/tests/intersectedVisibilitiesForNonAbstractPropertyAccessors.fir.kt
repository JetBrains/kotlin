// ISSUE: KT-66046

open class A1 {
    var a: Int = 10
        protected set
}

interface I1 {
    var a: Int
}

abstract class <!CANNOT_WEAKEN_ACCESS_PRIVILEGE_WARNING!>B1<!> : A1(), I1

open class A2 {
    protected fun foo(): Int = 10
}

interface I2 {
    fun foo(): Int
}

abstract class <!CANNOT_WEAKEN_ACCESS_PRIVILEGE!>B2<!> : A2(), I2
