// !LANGUAGE: +ContextReceivers
// WITH_STDLIB

interface C

fun C.foo(body: () -> Unit) {}

context(C)
class A {
    val foo = <!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER, RECURSION_IN_IMPLICIT_TYPES!>foo<!> {}
}

fun C.test() {
    object {
        val foo = foo {}
    }
}
