enum class E {
    E1;
    
    fun foo() = {
        test.lineNumber()
    }
    
    val prop = test.lineNumber()
}
