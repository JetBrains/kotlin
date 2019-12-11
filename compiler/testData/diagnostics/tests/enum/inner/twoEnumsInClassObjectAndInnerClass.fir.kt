class A {
    companion object {
        enum class E { ENTRY }  // OK
    }
    
    inner class B {
        enum class E { ENTRY }
    }
}
