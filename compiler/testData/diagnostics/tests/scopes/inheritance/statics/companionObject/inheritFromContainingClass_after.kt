// IGNORE_REVERSED_RESOLVE
// !LANGUAGE: +ProhibitVisibilityOfNestedClassifiersFromSupertypesOfCompanion
// !DIAGNOSTICS: -UNUSED_VARIABLE

// FILE: J.java
public class J {
    public static void foo() {}
}

// FILE: test.kt
open class A<T> : J() {
    init {
        foo()
        bar()
        val a: Int = <!TYPE_MISMATCH!><!DEBUG_INFO_LEAKING_THIS!>baz<!>()<!>
        val b: T = <!DEBUG_INFO_LEAKING_THIS!>baz<!>()
    }

    fun test1() {
        foo()
        bar()
        val a: Int = <!TYPE_MISMATCH!>baz()<!>
        val b: T = baz()
    }

    fun baz(): T = null!!

    object O {
        fun test() {
            foo()
            bar()
            val a: Int = baz()
            val b: <!UNRESOLVED_REFERENCE!>T<!> = baz()
        }
    }

    companion object : A<Int>() {
        init {
            foo()
            bar()
            val a: Int = baz()
            val b: <!UNRESOLVED_REFERENCE!>T<!> = baz()
        }

        fun test() {
            foo()
            bar()
            val a: Int = baz()
            val b: <!UNRESOLVED_REFERENCE!>T<!> = baz()
        }

        fun bar() {}
    }
}
