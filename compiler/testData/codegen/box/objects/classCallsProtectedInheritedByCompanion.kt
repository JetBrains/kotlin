// IGNORE_BACKEND_FIR: JVM_IR
open class A {
    protected fun foo() = "OK"
}

class B {
    companion object : A()

    fun bar() = foo()
}

fun box() = B().bar()
