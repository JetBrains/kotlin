class A {
    inner class B {
        <!NESTED_CLASS_NOT_ALLOWED!>class C<!>
    }
    
    fun foo() {
        class B {
            <!NESTED_CLASS_NOT_ALLOWED!>class C<!>
        }
    }
}

fun foo() {
    class B {
        <!NESTED_CLASS_NOT_ALLOWED!>class C<!>
    }
}


enum class E {
    E1 {
        // Not allowed in Java, but no reason to disallow in Kotlin
        class D
    }
}
