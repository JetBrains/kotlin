// IGNORE_REVERSED_RESOLVE
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
        <!DEPRECATED_ACCESS_BY_SHORT_NAME!>foo()<!>
        bar()
    }

    fun test2() {
        <!DEPRECATED_ACCESS_BY_SHORT_NAME!>foo()<!>
        bar()
    }

    object O {
        fun test() {
            <!DEPRECATED_ACCESS_BY_SHORT_NAME!>foo()<!>
            bar()
        }
    }

    companion object {
        init {
            <!DEPRECATED_ACCESS_BY_SHORT_NAME!>foo()<!>
            bar()
        }

        fun test() {
            <!DEPRECATED_ACCESS_BY_SHORT_NAME!>foo()<!>
            bar()
        }

        fun bar() {}
    }
}

