// TARGET_BACKEND: JVM_IR
// IGNORE_BACKEND_FIR: JVM_IR

class A {
    val ok = "OK"

    context(A)
    fun A.f() = ok
}

fun box(): String = with(A()) {
    f()
}