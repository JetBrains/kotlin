// IGNORE_BACKEND_FIR: JVM_IR
class A {
    private fun foo() = "OK"

    fun bar() = (A::foo)(this)
}

fun box() = A().bar()
