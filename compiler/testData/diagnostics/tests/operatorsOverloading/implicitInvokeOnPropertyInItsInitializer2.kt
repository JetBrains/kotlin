// RUN_PIPELINE_TILL: BACKEND
// DIAGNOSTICS: -CONTEXT_RECEIVERS_DEPRECATED, -CONTEXT_CLASS_OR_CONSTRUCTOR
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
