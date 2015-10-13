interface A<T> {
    fun foo(a: T)
}

interface B<E> {
    fun foo(b: E)
}

<!DIFFERENT_NAMES_FOR_THE_SAME_PARAMETER_IN_SUPERTYPES!>interface C<!><U> : A<U>, B<U> { // Warning here, this is correct
}

fun test(c: C<Int>) {
    c.foo(<!NAME_FOR_AMBIGUOUS_PARAMETER!>a<!> = 1)
    c.foo(<!NAME_FOR_AMBIGUOUS_PARAMETER!>b<!> = 1)
}