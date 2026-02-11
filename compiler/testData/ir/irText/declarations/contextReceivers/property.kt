// IGNORE_BACKEND_K2: ANY
// FIR_IDENTICAL
// LANGUAGE: +ContextReceivers, -ContextParameters

interface A {
    fun a(): Int
}
interface B {
    fun b(): Int
}

context(A, B)
val c get() = a() + b()
