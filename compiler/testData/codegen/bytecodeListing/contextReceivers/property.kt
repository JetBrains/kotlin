// !LANGUAGE: +ContextReceivers
// TARGET_BACKEND: JVM_IR

interface A {
    fun a(): Int
}
interface B {
    fun b(): Int
}

context(A, B)
val c get() = a() + b()
