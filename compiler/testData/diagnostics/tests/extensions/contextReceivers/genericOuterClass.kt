context(T) class A<T>

context(Collection<P>) class B<P>

fun Int.foo() {
    A<Int>()
    <!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>A<!><String>()
}

fun Collection<Int>.bar() {
    B<Int>()
    <!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>B<!><String>()
}