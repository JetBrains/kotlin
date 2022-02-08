// !LANGUAGE: +ContextReceivers
// TARGET_BACKEND: JVM_IR
// IGNORE_BACKEND_FIR: JVM_IR
// FIR status: context receivers aren't yet supported

class A {
    val ok = "OK"
}

context(A)
class B {
    fun result() = ok
}

fun box() = with(A()) {
    B().result()
}