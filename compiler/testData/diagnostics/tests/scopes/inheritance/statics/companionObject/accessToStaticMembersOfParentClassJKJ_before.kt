// IGNORE_REVERSED_RESOLVE
// !LANGUAGE: -ProhibitVisibilityOfNestedClassifiersFromSupertypesOfCompanion
// FILE: J.java
public class J {
    public static void foo() {}
}

// FILE: J2.java
public class J2 extends A {
    public static void boo() {}
}

// FILE: test.kt
open class A {
    companion object : J() {
        fun bar() {}
    }
}

class B : J2() {
    init {
        <!DEPRECATED_ACCESS_BY_SHORT_NAME!>foo()<!>
        bar()
        boo()
    }

    fun test2() {
        <!DEPRECATED_ACCESS_BY_SHORT_NAME!>foo()<!>
        bar()
        boo()
    }

    object O {
        fun test() {
            <!DEPRECATED_ACCESS_BY_SHORT_NAME!>foo()<!>
            bar()
            boo()
        }
    }

    companion object {
        init {
            <!DEPRECATED_ACCESS_BY_SHORT_NAME!>foo()<!>
            bar()
            boo()
        }

        fun test() {
            <!DEPRECATED_ACCESS_BY_SHORT_NAME!>foo()<!>
            bar()
            boo()
        }

        fun bar() {}
    }
}
