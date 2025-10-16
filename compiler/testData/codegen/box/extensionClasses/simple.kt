// LANGUAGE: +ContextReceivers
// TARGET_BACKEND: JVM_IR
// IGNORE_BACKEND_K2: ANY

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
