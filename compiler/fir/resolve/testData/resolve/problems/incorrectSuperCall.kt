interface A {
    fun bar() {}
}
open class B {
    open fun foo() {}

    open fun bar() {}
}

class C : A, B() {
    override fun foo() {
        super.foo()

        super.<!AMBIGUITY!>bar<!>() // should be ambiguity (NB: really we should have overridden bar in C)
    }
}
