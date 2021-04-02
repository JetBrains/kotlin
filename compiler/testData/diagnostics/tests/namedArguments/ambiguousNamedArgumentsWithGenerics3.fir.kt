interface A<T> {
    fun foo(a: T)
}

interface B<E> {
    fun foo(b: E)
}

interface C<U> : A<U>, B<U> { // Warning here, this is correct
}

fun test(c: C<Int>) {
    c.foo(a = 1)
    c.foo(<!NAMED_PARAMETER_NOT_FOUND!>b<!> = 1<!NO_VALUE_FOR_PARAMETER!>)<!>
}
