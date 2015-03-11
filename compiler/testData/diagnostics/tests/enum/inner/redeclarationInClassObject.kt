class A {
    enum class <!REDECLARATION!>E<!> {
        ENTRY
    }
    
    default object {
        enum class <!REDECLARATION!>E<!> {
            ENTRY2
        }
    }
}
