// FIR_IDENTICAL
// !LANGUAGE: +ContextReceivers

// MUTE_SIGNATURE_COMPARISON_K2: ANY
// ^ KT-57435

interface A {
    fun a(): Int
}
interface B {
    fun b(): Int
}

context(A, B)
val c get() = a() + b()
