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
        <!DEPRECATED_ACCESS_BY_SHORT_NAME!>C()<!>
    }

    object Z {
        init {
            <!RESOLUTION_TO_CLASSIFIER!>B<!>().<!DEBUG_INFO_MISSING_UNRESOLVED!>foo<!>()
            <!RESOLUTION_TO_CLASSIFIER!>B<!>().<!DEBUG_INFO_MISSING_UNRESOLVED!>bar<!>()

            <!RESOLUTION_TO_CLASSIFIER!>D<!>()
            <!DEPRECATED_ACCESS_BY_SHORT_NAME!>C()<!>
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
            <!RESOLUTION_TO_CLASSIFIER!>D<!>().<!DEBUG_INFO_MISSING_UNRESOLVED!>f<!>()
        }
    }
}
