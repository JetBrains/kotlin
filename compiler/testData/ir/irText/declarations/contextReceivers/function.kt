// FIR_IDENTICAL
// !LANGUAGE: +ContextReceivers
// IGNORE_BACKEND_K2: JS_IR
// IGNORE_BACKEND_K2: JS_IR_ES6

class C {
    val c = 42
}

context(C)
fun foo() {
    c
}

fun bar(c: C) {
    with(c) {
        foo()
    }
}
