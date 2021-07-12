context(T) class A<T>

context(Collection<P>) class B<P>

fun Int.foo() {
    A<Int>()
    <!NO_CONTEXT_RECEIVER!>A<String>()<!>
}

fun Collection<Int>.bar() {
    B<Int>()
    <!NO_CONTEXT_RECEIVER!>B<String>()<!>
}