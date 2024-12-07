// RUN_PIPELINE_TILL: FRONTEND
interface A<T> {
    fun foo(a: T)
}

interface B {
    fun foo(b: Int)
}

<!DIFFERENT_NAMES_FOR_THE_SAME_PARAMETER_IN_SUPERTYPES!>interface C<!> : A<Int>, B { // Warning here, this is correct
}

fun test(c: C) {
    c.foo(<!NAME_FOR_AMBIGUOUS_PARAMETER!>a<!> = 1)
    c.foo(<!NAME_FOR_AMBIGUOUS_PARAMETER!>b<!> = 1)
}