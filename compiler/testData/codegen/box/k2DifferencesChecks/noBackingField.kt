// ORIGINAL: /compiler/testData/diagnostics/tests/extensions/contextReceivers/noBackingField.fir.kt
// WITH_STDLIB
// !LANGUAGE: +ContextReceivers

interface A {
    fun a(): Int
}
interface B {
    fun b(): Int
}

context(A)
val a = 1

context(A, B)
var b = 2

context(A, B)
val c get() = a() + b()


fun box() = "OK"
