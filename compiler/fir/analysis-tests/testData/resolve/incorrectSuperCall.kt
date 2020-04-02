interface A {
    fun bar() {}

    fun baz()
}
open class B {
    open fun foo() {}

    open fun bar() {}

    open fun baz() {}
}

class C : A, B() {
    override fun foo() {
        super.foo()

        super.<!AMBIGUITY!>bar<!>() // should be ambiguity (NB: really we should have overridden bar in C)

        super.baz() // Ok
        baz()       // Ok
    }
}
