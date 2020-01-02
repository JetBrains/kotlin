// !LANGUAGE: -NestedClassesInEnumEntryShouldBeInner

class A {
    inner class B {
        class C
    }
    
    fun foo() {
        class B {
            class C
        }
    }
}

fun foo() {
    class B {
        class C
    }
}


enum class E {
    E1 {
        class D
    }
}
