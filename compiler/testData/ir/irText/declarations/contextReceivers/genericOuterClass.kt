// FIR_IDENTICAL
// !LANGUAGE: +ContextReceivers

// IGNORE_BACKEND_K1: JS_IR
// IGNORE_BACKEND_K1: JS_IR_ES6

// MUTE_SIGNATURE_COMPARISON_K2: ANY
// ^ KT-57429


context(T) class A<T>

context(Collection<P>) class B<P>

fun Int.foo() {
    A<Int>()
}

fun Collection<Int>.bar() {
    B<Int>()
}
