interface A {
    fun bar() {}
}
open class B {
    open fun foo() {}

    open fun bar() {}
}

class C : A, B() {
    override fun foo() {
        super.<!UNRESOLVED_REFERENCE!>foo<!>()

        super.bar() // should be ambiguity
    }
}
