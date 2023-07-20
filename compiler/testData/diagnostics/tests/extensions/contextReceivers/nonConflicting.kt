// FIR_IDENTICAL
// !LANGUAGE: +ContextReceivers

interface A
interface B
interface C
interface D

context(A, B)
fun f(): Unit = TODO()

context(C, D)
fun f(): Unit = TODO()

fun test(a: A, b: B) {
    with(a) {
        with(b) {
            f()
        }
    }
}