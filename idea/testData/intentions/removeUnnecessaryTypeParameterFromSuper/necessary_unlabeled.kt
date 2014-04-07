// IS_APPLICABLE: false
trait A {
    fun foo() {
    }
}

trait B {
    fun foo() {
    }
}

class D() : A, B {
    override fun foo() {
        su<caret>per<A>.foo()
        super<B>.foo()
    }
}
