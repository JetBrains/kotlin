// !DIAGNOSTICS: -UNUSED_PARAMETER
class A {
    fun foo() = 1
    constructor(x: Int)
    constructor(x: Int, y: Int, z: Int = x + foo() + this.foo()) :
        <!INAPPLICABLE_CANDIDATE!>this<!>(x <!AMBIGUITY!>+<!> <!UNRESOLVED_REFERENCE!>foo<!>() + this.<!UNRESOLVED_REFERENCE!>foo<!>())
}
