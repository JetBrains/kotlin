class A {
    default object {
        val prop = test.lineNumber()
        
        fun foo(): Int {
            return test.lineNumber()
        }
    }
}
