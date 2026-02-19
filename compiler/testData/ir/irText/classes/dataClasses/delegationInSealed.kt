// FIR_IDENTICAL
// WITH_STDLIB
// IGNORE_BACKEND_K1: JS_IR

sealed class A : CharSequence {
    data class B(val c: CharSequence) : A(), CharSequence by c
}
