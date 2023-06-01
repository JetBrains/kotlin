// FIR_IDENTICAL
// !LANGUAGE: +ContextReceivers

// MUTE_SIGNATURE_COMPARISON_K2: JVM_IR
// ^ KT-57429

context(T) class A<T>

context(Collection<P>) class B<P>

fun Int.foo() {
    A<Int>()
}

fun Collection<Int>.bar() {
    B<Int>()
}
