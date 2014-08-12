class A {
    inner class B {
        <!NESTED_CLASS_NOT_ALLOWED!>enum class E<!> {
            ENTRY
        }
    }
}
