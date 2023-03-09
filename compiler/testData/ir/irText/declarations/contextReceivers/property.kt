// FIR_IDENTICAL
// !LANGUAGE: +ContextReceivers

// NO_SIGNATURE_DUMP
// ^KT-57435

interface A {
    fun a(): Int
}
interface B {
    fun b(): Int
}

context(A, B)
val c get() = a() + b()
