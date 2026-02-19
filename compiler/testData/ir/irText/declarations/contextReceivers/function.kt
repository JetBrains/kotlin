// IGNORE_BACKEND_K2: ANY
// FIR_IDENTICAL
// LANGUAGE: +ContextReceivers

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
