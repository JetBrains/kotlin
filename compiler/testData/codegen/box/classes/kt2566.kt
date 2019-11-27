// IGNORE_BACKEND_FIR: JVM_IR
open class A {
    open fun foo() = "OK"
}

open class B : A() {
    override fun foo() = super.foo()
}

interface I

class C : I, B() {
    override fun foo() = super<B>.foo()
}

fun box() = C().foo()
