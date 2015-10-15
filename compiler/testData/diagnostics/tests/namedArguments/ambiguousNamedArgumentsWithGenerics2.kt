interface A {
    fun <E> foo(a: E)
}

interface B {
    fun <T> foo(b: T)
}

<!DIFFERENT_NAMES_FOR_THE_SAME_PARAMETER_IN_SUPERTYPES!>interface C<!> : A, B { // Warning here, this is correct
}

fun test(c: C) {
    c.foo(<!NAME_FOR_AMBIGUOUS_PARAMETER!>a<!> = 1)
    c.foo(<!NAME_FOR_AMBIGUOUS_PARAMETER!>b<!> = 1)
}