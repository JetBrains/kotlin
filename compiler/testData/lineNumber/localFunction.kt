fun foo() {
    test.lineNumber()
    fun bar() {
        test.lineNumber()
    }
    test.lineNumber()
    bar()
    test.lineNumber()
}
