class A {
    class object {
        enum class E { ENTRY }  // OK
    }
    
    inner class B {
        enum class <!NESTED_CLASS_NOT_ALLOWED!>E<!> { ENTRY }
    }
}
