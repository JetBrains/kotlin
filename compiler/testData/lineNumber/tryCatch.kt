fun foo() {
    try {
        test.lineNumber()
    } catch (e: Exception) {
        test.lineNumber()
    }
}
