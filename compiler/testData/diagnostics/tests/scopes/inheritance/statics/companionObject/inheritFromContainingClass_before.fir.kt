// !LANGUAGE: -ProhibitVisibilityOfNestedClassifiersFromSupertypesOfCompanion
// !WITH_NEW_INFERENCE
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
        val a: Int = baz()
        val b: T = baz()
    }

    fun test1() {
        foo()
        bar()
        val a: Int = baz()
        val b: T = baz()
    }

    fun baz(): T = null!!

    object O {
        fun test() {
            <!UNRESOLVED_REFERENCE!>foo<!>()
            bar()
            val a: Int = <!UNRESOLVED_REFERENCE!>baz<!>()
            val b: T = <!UNRESOLVED_REFERENCE!>baz<!>()
        }
    }

    companion object : A<Int>() {
        init {
            foo()
            bar()
            val a: Int = baz()
            val b: T = baz()
        }

        fun test() {
            foo()
            bar()
            val a: Int = baz()
            val b: T = baz()
        }

        fun bar() {}
    }
}
