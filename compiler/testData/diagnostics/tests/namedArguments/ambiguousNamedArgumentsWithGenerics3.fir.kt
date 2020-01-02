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
    c.<!INAPPLICABLE_CANDIDATE!>foo<!>(b = 1)
}