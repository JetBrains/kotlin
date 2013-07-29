open class A {
    open fun <caret>foo() {

    }
}

trait Z {
    fun foo()
}

class B: A, Z {
    override fun foo() {

    }
}