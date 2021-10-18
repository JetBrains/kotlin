// !LANGUAGE: +ContextReceivers
// TARGET_BACKEND: JVM_IR
// IGNORE_BACKEND_FIR: JVM_IR
// IGNORE_BACKEND: JVM

interface A {
    fun a(): Int
}
interface B {
    fun b(): Int
}

context(A, B)
val c get() = a() + b()