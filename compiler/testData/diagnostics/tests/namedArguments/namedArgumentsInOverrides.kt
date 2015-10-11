interface A {
    fun foo(a1: Int, a2: Double)
}

interface B {
    fun foo(b1: Int, b2: Double)
}

<!DIFFERENT_NAMES_FOR_THE_SAME_PARAMETER_IN_SUPERTYPES, DIFFERENT_NAMES_FOR_THE_SAME_PARAMETER_IN_SUPERTYPES!>interface C<!> : A, B { // Warning here, this is correct, C.foo has no named parameters
}

interface D : C {
    override fun foo(<!PARAMETER_NAME_CHANGED_ON_OVERRIDE!>d1<!>: Int, <!PARAMETER_NAME_CHANGED_ON_OVERRIDE!>d2<!>: Double)
}

fun test1(d: D) {
    d.foo(d1 = 1, d2 = 1.0)
}

fun test2(c: C) {
    c.foo(<!NAME_FOR_AMBIGUOUS_PARAMETER!>b1<!> = 1, <!NAME_FOR_AMBIGUOUS_PARAMETER!>b2<!> = 1.0)
}