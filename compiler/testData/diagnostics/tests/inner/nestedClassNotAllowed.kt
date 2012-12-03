class A {
    inner class B {
        class <!NESTED_CLASS_NOT_ALLOWED!>C<!>
    }
    
    fun foo() {
        class B {
            class <!NESTED_CLASS_NOT_ALLOWED!>C<!>
        }
    }
}

fun foo() {
    class B {
        class <!NESTED_CLASS_NOT_ALLOWED!>C<!>
    }
}


enum class E {
    E1 {
        // Not allowed in Java, but no reason to disallow in Kotlin
        class D
    }
}
