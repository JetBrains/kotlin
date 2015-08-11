open class <caret>A {
    // INFO: {"checked": "true", "toAbstract": "true"}
    private open fun foo() {

    }

    inner class B : A() {
        override fun foo() {

        }
    }
}