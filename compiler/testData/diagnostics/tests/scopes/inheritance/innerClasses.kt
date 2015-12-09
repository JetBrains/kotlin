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
        C()
    }

    object Z {
        init {
            <!UNRESOLVED_REFERENCE!>B<!>().<!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE!>foo<!>()
            <!UNRESOLVED_REFERENCE!>B<!>().<!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE!>bar<!>()

            <!UNRESOLVED_REFERENCE!>D<!>()
            C()
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
            <!UNRESOLVED_REFERENCE!>D<!>().<!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE!>f<!>()
        }
    }
}