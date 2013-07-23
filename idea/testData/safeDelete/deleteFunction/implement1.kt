trait A {
    fun foo()
}

class B: A {
    override fun <caret>foo() {

    }
}