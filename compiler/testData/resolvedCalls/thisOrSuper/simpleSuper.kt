open class A {
    open fun foo() {}
}

class B: A() {
    override fun foo() {
        <caret>super.foo()
    }
}
