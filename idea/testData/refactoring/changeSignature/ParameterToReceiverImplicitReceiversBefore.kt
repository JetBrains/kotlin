class C {
    fun <caret>foo(s: String) {
        with(1) {
            bar()
        }
    }

    fun Int.bar() {}
}