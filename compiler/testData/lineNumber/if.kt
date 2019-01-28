fun foo() {
    if (test.lineNumber() > 0) {
        test.lineNumber()
    }
    
    if (test.lineNumber() > 0) else {
        test.lineNumber()
    }
    
    if (test.lineNumber() > 0) {
        test.lineNumber()
    } else {
        test.lineNumber()
    }
}
