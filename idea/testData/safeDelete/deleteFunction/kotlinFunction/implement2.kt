trait A {
    fun foo()
}

trait Z {
    fun foo()
}

class B: A, Z {
    override fun <caret>foo() {

    }
}