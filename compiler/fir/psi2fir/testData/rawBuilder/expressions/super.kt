interface A {
    fun foo() {}
}

interface B {
    fun foo() {}
    fun bar() {}
}

class C : A, B {
    override fun bar() {
        super.bar()
    }

    override fun foo() {
        super<A>.foo()
        super<B>.foo()
    }
}