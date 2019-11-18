// IGNORE_BACKEND_FIR: JVM_IR
class A {
    fun foo() = "OK"
}

fun A?.bar() = if (this != null) foo() else "FAIL"

fun box() = A().bar()
