// PROBLEM: none
open class A {
    open fun setFoo(s: String) = Unit
}

class B : A() {
    private var foo: String = ""

    <caret>override fun setFoo(s: String) = super.setFoo(s)
}