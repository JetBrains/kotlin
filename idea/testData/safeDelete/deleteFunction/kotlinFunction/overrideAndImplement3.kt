open class A {
    open fun foo() {

    }
}

trait Z {
    fun <caret>foo()
}

class B: A(), Z {
    override fun foo() {

    }
}