open class <caret>A {
    // INFO: {"checked": "true", "toAbstract": "true"}
    private fun foo() {

    }
}

class B : A {
    fun foo() {

    }
}