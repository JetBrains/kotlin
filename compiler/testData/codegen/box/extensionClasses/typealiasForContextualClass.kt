// LANGUAGE: +ContextReceivers
// TARGET_BACKEND: JVM_IR
// FIR status: context receivers aren't yet supported

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
