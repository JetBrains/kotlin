interface A {
    fun bar() {}

    fun baz()
}
open class B {
    open fun foo() {}

    open fun bar() {}

    open fun baz() {}
}

<!MANY_IMPL_MEMBER_NOT_IMPLEMENTED!>class C<!> : A, B() {
    override fun foo() {
        super.foo()

        super.<!UNRESOLVED_REFERENCE!>bar<!>() // should be ambiguity (NB: really we should have overridden bar in C)

        super.baz() // Ok
        baz()       // Ok
    }
}
