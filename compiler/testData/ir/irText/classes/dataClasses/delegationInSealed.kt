// FIR_IDENTICAL
// WITH_STDLIB
// SKIP_KLIB_TEST
// IGNORE_BACKEND_K1: JS_IR

// KT-64271
// IGNORE_BACKEND_K2: JVM_IR


sealed class A : CharSequence {
    data class B(val c: CharSequence) : A(), CharSequence by c
}
