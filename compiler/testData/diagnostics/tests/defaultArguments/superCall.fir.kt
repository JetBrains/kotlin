// !DIAGNOSTICS: -UNUSED_PARAMETER -ABSTRACT_SUPER_CALL

abstract class A {
    open fun foo(a: String = "default") {
    }

    final fun foo2(a: String = "default") {
    }

    abstract fun foo3(a: String = "default")
}

open class B : A() {
    fun test() {
        super.foo("123")
        super.foo()

        super.foo2("123")
        super.foo2()

        super.<!ABSTRACT_SUPER_CALL!>foo3<!>("123")
        super.<!ABSTRACT_SUPER_CALL!>foo3<!>()
    }

    override fun foo3(a: String) {
        throw UnsupportedOperationException()
    }
}