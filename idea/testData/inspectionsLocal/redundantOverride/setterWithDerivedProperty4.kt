open class A {
    open fun setFoo(s: String) = Unit
}

class B : A() {
    private var foo: Int = 42

    <caret>override fun setFoo(s: String) = super.setFoo(s)
}