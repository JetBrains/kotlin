class Test {
    fun bar(): Int = 42
    fun m() {
        val f = this:<caret>:bar
    }
}