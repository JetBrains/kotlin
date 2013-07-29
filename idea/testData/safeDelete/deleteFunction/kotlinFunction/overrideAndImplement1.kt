open class A {
    open fun foo() {

    }
}

trait Z {
    fun foo()
}

class B: A, Z {
    override fun <caret>foo() {

    }
}