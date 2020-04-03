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
        foo()
        bar()
        baz()
    }

    fun test1() {
        foo()
        bar()
        baz()
    }

    object O {
        fun test() {
            foo()
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
