// !DIAGNOSTICS: -UNUSED_PARAMETER
open class B(x: Int) {
    fun foo() = 1
}
class A : B {
    constructor(x: Int, y: Int = x + foo() + this.foo() + super.foo()) :
        <!INAPPLICABLE_CANDIDATE!>super<!>(x <!AMBIGUITY!>+<!> <!UNRESOLVED_REFERENCE!>foo<!>() + this.<!UNRESOLVED_REFERENCE!>foo<!>() + super.foo())
}
