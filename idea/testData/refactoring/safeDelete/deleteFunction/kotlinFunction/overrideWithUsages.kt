open class A {
    open fun <caret>foo() {

    }
}

class B: A {
    fun bar() {
        foo()
    }

    override fun foo() {

    }
}