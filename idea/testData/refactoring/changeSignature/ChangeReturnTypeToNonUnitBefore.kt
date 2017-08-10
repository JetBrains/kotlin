open class A {
    open fun <caret>foo() {

    }
}

class B : A() {
    override fun foo() {

    }
}