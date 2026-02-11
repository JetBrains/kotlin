// FIR_IDENTICAL
// LANGUAGE: +ContextReceivers, -ContextParameters
// IGNORE_BACKEND_K2: ANY

context(T) class A<T>

context(Collection<P>) class B<P>

fun Int.foo() {
    A<Int>()
}

fun Collection<Int>.bar() {
    B<Int>()
}
