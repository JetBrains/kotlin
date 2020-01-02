open class A {
    open fun foo() {}
}

interface ATrait : A {

    override fun foo() {
        super<A>.foo()
    }
}