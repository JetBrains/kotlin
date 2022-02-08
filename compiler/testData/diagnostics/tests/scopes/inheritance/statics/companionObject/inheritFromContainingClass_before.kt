// !LANGUAGE: -ProhibitVisibilityOfNestedClassifiersFromSupertypesOfCompanion
// !DIAGNOSTICS: -UNUSED_VARIABLE

// FILE: J.java
public class J {
    public static void foo() {}
}

// FILE: test.kt
open class A<T> : J() {
    init {
        <!DEPRECATED_ACCESS_BY_SHORT_NAME!>foo()<!>
        bar()
        val a: Int = <!TYPE_MISMATCH!><!DEBUG_INFO_LEAKING_THIS!>baz<!>()<!>
        val b: T = <!DEBUG_INFO_LEAKING_THIS!>baz<!>()
    }

    fun test1() {
        <!DEPRECATED_ACCESS_BY_SHORT_NAME!>foo()<!>
        bar()
        val a: Int = <!TYPE_MISMATCH!>baz()<!>
        val b: T = baz()
    }

    fun baz(): T = null!!

    object O {
        fun test() {
            <!DEPRECATED_ACCESS_BY_SHORT_NAME!>foo()<!>
            bar()
            val a: Int = baz()
            val b: <!UNRESOLVED_REFERENCE!>T<!> = baz()
        }
    }

    companion object : A<Int>() {
        init {
            <!DEPRECATED_ACCESS_BY_SHORT_NAME!>foo()<!>
            bar()
            val a: Int = baz()
            val b: <!UNRESOLVED_REFERENCE!>T<!> = baz()
        }

        fun test() {
            <!DEPRECATED_ACCESS_BY_SHORT_NAME!>foo()<!>
            bar()
            val a: Int = baz()
            val b: <!UNRESOLVED_REFERENCE!>T<!> = baz()
        }

        fun bar() {}
    }
}
