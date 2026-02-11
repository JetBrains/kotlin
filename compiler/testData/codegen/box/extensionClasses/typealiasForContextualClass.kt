// LANGUAGE: +ContextReceivers, -ContextParameters
// TARGET_BACKEND: JVM_IR
// IGNORE_BACKEND_K2: ANY
// IGNORE_HEADER_MODE: JVM_IR

class A {
    val ok = "OK"
}

context(A)
class B {
    val result = ok
}

typealias C = B

fun box(): String =
    with(A()) { C().result }
