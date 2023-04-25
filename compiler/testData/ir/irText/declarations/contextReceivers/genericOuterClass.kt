// FIR_IDENTICAL
// !LANGUAGE: +ContextReceivers

// IGNORE_BACKEND_K1: JS_IR
// IGNORE_BACKEND_K1: JS_IR_ES6

context(T) class A<T>

context(Collection<P>) class B<P>

fun Int.foo() {
    A<Int>()
}

fun Collection<Int>.bar() {
    B<Int>()
}
