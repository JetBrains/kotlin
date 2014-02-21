fun foo() {
    fun bar() : Boolean {
        return true
    }

    bar<caret>()
}
