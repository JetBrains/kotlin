// FIR_IDENTICAL
// !LANGUAGE: +ProhibitVisibilityOfNestedClassifiersFromSupertypesOfCompanion
// FILE: J.java
public class J {
    public static void foo() {}
}

// FILE: test.kt
class A {
    init {
        <!UNRESOLVED_REFERENCE!>foo<!>()
        bar()
    }

    fun test1() {
        <!UNRESOLVED_REFERENCE!>foo<!>()
        bar()
    }

    object O {
        fun test() {
            <!UNRESOLVED_REFERENCE!>foo<!>()
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
