// !LANGUAGE: -ProhibitVisibilityOfNestedClassifiersFromSupertypesOfCompanion
// FILE: J.java
public class J {
    public static void foo() {}
}

// FILE: test.kt
class A {
    init {
        <!DEPRECATED_ACCESS_BY_SHORT_NAME!>foo()<!>
        bar()
    }

    fun test1() {
        <!DEPRECATED_ACCESS_BY_SHORT_NAME!>foo()<!>
        bar()
    }

    object O {
        fun test() {
            <!DEPRECATED_ACCESS_BY_SHORT_NAME!>foo()<!>
            bar()
        }
    }

    companion object : J() {
        init {
            foo()
            bar()
        }

        fun test() {
            foo()
            bar()
        }

        fun bar() {}
    }
}
