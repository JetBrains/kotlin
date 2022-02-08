// !LANGUAGE: +ContextReceivers

context(T) class A<T>

context(Collection<P>) class B<P>

fun Int.foo() {
    A<Int>()
    A<String>()
}

fun Collection<Int>.bar() {
    B<Int>()
    B<String>()
}