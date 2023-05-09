// FIR_IDENTICAL
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
        baz()
    }

    fun test1() {
        <!UNRESOLVED_REFERENCE!>foo<!>()
        bar()
        baz()
    }

    object O {
        fun test() {
            <!UNRESOLVED_REFERENCE!>foo<!>()
            bar()
            baz()
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
