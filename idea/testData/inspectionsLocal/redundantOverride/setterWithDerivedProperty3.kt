open class A {
    open fun setFoo(s: String, i: Int) = Unit
}

class B : A() {
    private var foo: String = ""

    <caret>override fun setFoo(s: String, i: Int) = super.setFoo(s, i)
}