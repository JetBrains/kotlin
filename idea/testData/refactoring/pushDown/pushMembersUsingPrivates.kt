open class <caret>A {
    private fun foo() {

    }

    // INFO: {"checked": "true"}
    fun bar() {
        foo()
    }
}

class B : A() {

}