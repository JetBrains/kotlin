// !LANGUAGE: +ProhibitVisibilityOfNestedClassifiersFromSupertypesOfCompanion
// FILE: J.java
public class J {
    public static void foo() {}
}

// FILE: test.kt

open class B : J() {
    fun baz() {}
}

class A {
    init {
        <!UNRESOLVED_REFERENCE!>foo<!>()
        bar()
        <!UNRESOLVED_REFERENCE!>baz<!>()
    }

    fun test1() {
        <!UNRESOLVED_REFERENCE!>foo<!>()
        bar()
        <!UNRESOLVED_REFERENCE!>baz<!>()
    }

    object O {
        fun test() {
            <!UNRESOLVED_REFERENCE!>foo<!>()
            bar()
            <!UNRESOLVED_REFERENCE!>baz<!>()
        }
    }


    companion object : B() {
        init {
            foo()
            bar()
            baz()
        }

        fun test() {
            foo()
            bar()
            baz()
        }

        fun bar() {}
    }
}
