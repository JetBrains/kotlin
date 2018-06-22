// !LANGUAGE: +NestedClassesInEnumEntryShouldBeInner

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
        <!NESTED_CLASS_NOT_ALLOWED!>class D<!>
    }
}
