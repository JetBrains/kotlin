// FIR_IDENTICAL
class A {
    companion object {
        enum class E { ENTRY }  // OK
    }
    
    inner class B {
        <!NESTED_CLASS_NOT_ALLOWED!>enum class E<!> { ENTRY }
    }
}
