// !LANGUAGE: +ContextReceivers
// TARGET_BACKEND: JVM_IR
// IGNORE_BACKEND_K2: JVM_IR
// IGNORE_BACKEND: JVM

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