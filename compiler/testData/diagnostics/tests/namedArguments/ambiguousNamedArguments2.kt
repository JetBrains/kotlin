interface A {
    fun foo(a1: Int, a2: Double)
    fun bar(a1: Int, a2: Double, a3: String)
    fun baz(a1: Int, a2: Double, a3: String, a4: Int, a5: String)
}

interface B {
    fun foo(b1: Int, b2: Double)
    fun bar(a1: Int, a2: Double, b3: String)
    fun baz(a1: Int, b2: Double, a3: String, b4: Int, a5: String)
}

<!DIFFERENT_NAMES_FOR_THE_SAME_PARAMETER_IN_SUPERTYPES, DIFFERENT_NAMES_FOR_THE_SAME_PARAMETER_IN_SUPERTYPES, DIFFERENT_NAMES_FOR_THE_SAME_PARAMETER_IN_SUPERTYPES, DIFFERENT_NAMES_FOR_THE_SAME_PARAMETER_IN_SUPERTYPES, DIFFERENT_NAMES_FOR_THE_SAME_PARAMETER_IN_SUPERTYPES!>interface C<!> : A, B { // Warning here, this is correct
}

fun test(c: C) {
    c.foo(<!NAME_FOR_AMBIGUOUS_PARAMETER!>b1<!> = 1, <!NAME_FOR_AMBIGUOUS_PARAMETER!>b2<!> = 1.0)
    c.foo(<!NAME_FOR_AMBIGUOUS_PARAMETER!>a1<!> = 1, <!NAME_FOR_AMBIGUOUS_PARAMETER!>b2<!> = 1.0)
    c.foo(<!NAME_FOR_AMBIGUOUS_PARAMETER!>a1<!> = 1, <!NAME_FOR_AMBIGUOUS_PARAMETER!>a2<!> = 1.0)
    c.foo(<!NAME_FOR_AMBIGUOUS_PARAMETER!>a1<!> = 1, <!NAME_FOR_AMBIGUOUS_PARAMETER!>a2<!> = 1.0)
    c.bar(a1 = 1, a2 = 1.0, <!NAME_FOR_AMBIGUOUS_PARAMETER!>b3<!>= "")
    c.baz(a1 = 1, <!NAME_FOR_AMBIGUOUS_PARAMETER!>b2<!> = 1.0, a3 = "", <!NAME_FOR_AMBIGUOUS_PARAMETER!>b4<!> = 2, a5 = "")
    c.baz(a1 = 1, <!NAME_FOR_AMBIGUOUS_PARAMETER!>a2<!> = 1.0, a3 = "", <!NAME_FOR_AMBIGUOUS_PARAMETER!>b4<!> = 2, a5 = "")
}