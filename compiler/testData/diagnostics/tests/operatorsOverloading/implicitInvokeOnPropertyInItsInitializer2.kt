// FIR_IDENTICAL
// LANGUAGE: +ContextReceivers
// WITH_STDLIB

interface C

fun C.foo(body: () -> Unit) {}

context(C)
class A {
    val foo = foo {}
}

fun C.test() {
    object {
        val foo = foo {}
    }
}
