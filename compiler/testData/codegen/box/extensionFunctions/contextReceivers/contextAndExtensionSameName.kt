// !LANGUAGE: +ContextReceivers
// TARGET_BACKEND: JVM_IR

class A {
    val ok = "OK"

    context(A)
    fun A.f() = ok
}

fun box(): String = with(A()) {
    f()
}