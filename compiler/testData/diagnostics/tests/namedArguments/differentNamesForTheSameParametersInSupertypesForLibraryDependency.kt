// FIR_IDENTICAL
// RUN_PIPELINE_TILL: BACKEND

// MODULE: lib

interface A {
    fun foo(x : Int)
}

interface B {
    fun foo(y : Int)
}

// MODULE: main(lib)

// K1 reports DIFFERENT_NAMES_FOR_THE_SAME_PARAMETER_IN_SUPERTYPES here, hence it's not a problem that K2 also reports that diagnostic
<!DIFFERENT_NAMES_FOR_THE_SAME_PARAMETER_IN_SUPERTYPES!>interface C<!> : A, B