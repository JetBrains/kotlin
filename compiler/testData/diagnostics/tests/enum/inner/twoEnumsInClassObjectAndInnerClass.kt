class A {
    class object {
        enum class E { ENTRY }  // OK
    }
    
    class B {
        <!ENUM_NOT_ALLOWED!>enum<!> class E { ENTRY }
    }
}
