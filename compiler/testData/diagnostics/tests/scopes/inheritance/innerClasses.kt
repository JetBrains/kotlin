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
            <!NESTED_CLASS_SHOULD_BE_QUALIFIED!>B<!>().<!UNRESOLVED_REFERENCE!>foo<!>() // todo: some resolve hacks
            <!NESTED_CLASS_SHOULD_BE_QUALIFIED!>B<!>().bar()

            <!NO_COMPANION_OBJECT, FUNCTION_EXPECTED!>D<!>()
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
            <!NO_COMPANION_OBJECT, FUNCTION_EXPECTED!>D<!>().<!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE!>f<!>()
        }
    }
}