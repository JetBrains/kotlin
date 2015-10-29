class C {
    fun String.<caret>foo() {
        with(1) {
            bar()
        }
    }

    fun Int.bar() {}
}