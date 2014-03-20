trait C {
    fun foo(a : Int)
}

trait D {
    fun foo(b : Int)
}

trait <!DIFFERENT_NAMES_FOR_THE_SAME_PARAMETER_IN_SUPERTYPES!>E<!> : C, D

trait F : C, D {
    override fun foo(<!PARAMETER_NAME_CHANGED_ON_OVERRIDE!>a<!> : Int) {
        throw UnsupportedOperationException()
    }
}