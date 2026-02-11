// LANGUAGE: +ContextReceivers, -ContextParameters
// TARGET_BACKEND: JVM_IR
// IGNORE_BACKEND_K2: ANY

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
