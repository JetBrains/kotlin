interface A {
    fun foo() = test.lineNumber()
    
    fun bar(): Int {
        return test.lineNumber()
    }
}
