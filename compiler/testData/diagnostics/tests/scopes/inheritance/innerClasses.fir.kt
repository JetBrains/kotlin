// IGNORE_REVERSED_RESOLVE
// !LANGUAGE: -ProhibitVisibilityOfNestedClassifiersFromSupertypesOfCompanion

open class A {
    inner class B {
        fun foo() {}
    }

    inner class D

    companion object {
        class B {
            fun bar() {}
        }

        class C
    }

    init {
        B().foo()
        B().<!UNRESOLVED_REFERENCE!>bar<!>()

        D()
        C()
    }
}

class E: A() {
    init {
        B().foo()
        B().<!UNRESOLVED_REFERENCE!>bar<!>()

        D()
        <!UNRESOLVED_REFERENCE!>C<!>()
    }

    object Z {
        init {
            <!RESOLUTION_TO_CLASSIFIER!>B<!>().<!UNRESOLVED_REFERENCE!>foo<!>()
            <!RESOLUTION_TO_CLASSIFIER!>B<!>().<!UNRESOLVED_REFERENCE!>bar<!>()

            <!RESOLUTION_TO_CLASSIFIER!>D<!>()
            <!UNRESOLVED_REFERENCE!>C<!>()
        }
    }
}

class F: A() {
    class B {
        fun fas() {}
    }
    inner class D {
        fun f() {}
    }

    init {
        B().fas()
        D().f()
    }

    companion object {
        init {
            B().fas()
            <!RESOLUTION_TO_CLASSIFIER!>D<!>().<!UNRESOLVED_REFERENCE!>f<!>()
        }
    }
}
