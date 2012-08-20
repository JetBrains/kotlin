fun foo() {
    while (test.lineNumber() > 0) {
        test.lineNumber()
    }
    
    do {
        test.lineNumber()
    } while (test.lineNumber() > 0)
}
