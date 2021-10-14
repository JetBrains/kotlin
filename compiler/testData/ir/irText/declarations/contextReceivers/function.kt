// !LANGUAGE: +ContextReceivers
// IGNORE_BACKEND_FIR: JVM_IR

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