// !LANGUAGE: +ContextReceivers
// TARGET_BACKEND: JVM_IR

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
