// IGNORE_BACKEND_FIR: JVM_IR
open class A {
    open fun foo() = "FAIL"

    fun bar() = if (this is C) foo() else foo()
}

open class B : A()

open class C : B() {
    override fun foo() = "OK"
}

fun box() = C().bar()
