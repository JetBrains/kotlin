// IGNORE_REVERSED_RESOLVE
// !LANGUAGE: -ProhibitVisibilityOfNestedClassifiersFromSupertypesOfCompanion
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
        <!DEPRECATED_ACCESS_BY_SHORT_NAME!>foo()<!>
        bar()
        baz()
    }

    fun test1() {
        <!DEPRECATED_ACCESS_BY_SHORT_NAME!>foo()<!>
        bar()
        baz()
    }

    object O {
        fun test() {
            <!DEPRECATED_ACCESS_BY_SHORT_NAME!>foo()<!>
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
