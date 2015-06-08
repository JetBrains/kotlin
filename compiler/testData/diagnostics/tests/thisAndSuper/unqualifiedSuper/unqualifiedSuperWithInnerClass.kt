open class A {
    open fun foo() {}
}

interface B {
    fun bar() {}
}

interface Q {
    fun qux() {}
}

class C : A(), B {
    override fun foo() {
        super@C.foo()
    }

    override fun bar() {
        super@C.bar()
    }

    inner class D : A(), Q {
        override fun foo() {
            super@C.foo()
            super@D.foo()
        }

        override fun qux() {
            super@C.<!UNRESOLVED_REFERENCE!>qux<!>()
            super@D.qux()
        }
    }
}