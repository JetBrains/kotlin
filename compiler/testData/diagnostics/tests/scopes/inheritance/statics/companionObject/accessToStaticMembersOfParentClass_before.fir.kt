// !LANGUAGE: -ProhibitVisibilityOfNestedClassifiersFromSupertypesOfCompanion
// FILE: J.java
public class J {
    public static void foo() {}
}

// FILE: test.kt
open class A {
    companion object : J() {
        fun bar() {}
    }
}

class B : A() {
    init {
        <!UNRESOLVED_REFERENCE!>foo<!>()
        bar()
    }

    fun test2() {
        <!UNRESOLVED_REFERENCE!>foo<!>()
        bar()
    }

    object O {
        fun test() {
            <!UNRESOLVED_REFERENCE!>foo<!>()
            bar()
        }
    }

    companion object {
        init {
            <!UNRESOLVED_REFERENCE!>foo<!>()
            bar()
        }

        fun test() {
            <!UNRESOLVED_REFERENCE!>foo<!>()
            bar()
        }

        fun bar() {}
    }
}

