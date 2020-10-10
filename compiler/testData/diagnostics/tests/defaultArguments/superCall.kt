// !DIAGNOSTICS: -UNUSED_PARAMETER -ABSTRACT_SUPER_CALL

interface I {
    fun bar(): Any?
}

interface EI : I {
    override fun bar(): Any? {
        throw UnsupportedOperationException()
    }
}

abstract class A : I, EI {
    open fun foo(a: String = "default") {
    }

    final fun foo2(a: String = "default") {
    }

    abstract fun foo3(a: String = "default")
}

class B(i: I) : A(), EI, I by i {
    fun test() {
        super.foo("123")
        super.<!SUPER_CALL_WITH_DEFAULT_PARAMETERS!>foo<!>()

        super.foo2("123")
        super.<!SUPER_CALL_WITH_DEFAULT_PARAMETERS!>foo2<!>()

        super.foo3("123")
        super.<!SUPER_CALL_WITH_DEFAULT_PARAMETERS!>foo3<!>()
    }

    override fun foo3(a: String) {
        throw UnsupportedOperationException()
    }

    override fun bar(): Any? {
        return super<A>.bar()
    }
}