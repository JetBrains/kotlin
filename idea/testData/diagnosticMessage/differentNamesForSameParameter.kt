// !DIAGNOSTICS_NUMBER: 1
// !DIAGNOSTICS: DIFFERENT_NAMES_FOR_THE_SAME_PARAMETER_IN_SUPERTYPES

trait A {
    fun foo(x: Int)
}

trait B {
    fun foo(y: Int) {}
}

class differentNamesForSameParameter : A, B
