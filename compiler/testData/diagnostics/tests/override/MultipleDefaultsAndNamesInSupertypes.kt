trait A {
    fun foo(x: Int = 42): Int
}

open class B {
    fun foo(x: Int = 239) = x
}

trait C {
    fun foo(y: Int)
}

class <!MULTIPLE_DEFAULTS_INHERITED_FROM_SUPERTYPES_WHEN_NO_EXPLICIT_OVERRIDE, DIFFERENT_NAMES_FOR_THE_SAME_PARAMETER_IN_SUPERTYPES!>Z<!> : A, B(), C