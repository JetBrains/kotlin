open class A {
    open fun foo() = "OK"
}

open class B : A() {
    override fun foo() = super.foo()
}

trait I

class C : I, B() {
    override fun foo() = super<B>.foo()
}

fun box() = C().foo()
