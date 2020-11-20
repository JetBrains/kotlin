// !DIAGNOSTICS: -UNUSED_PARAMETER
open class B(x: Int) {
    open fun foo() = 1
}
class A : B {
    override fun foo() = 2
    constructor(x: Int, y: Int = x <!AMBIGUITY!>+<!> <!UNRESOLVED_REFERENCE!>foo<!>() + <!INSTANCE_ACCESS_BEFORE_SUPER_CALL!>this<!>.<!UNRESOLVED_REFERENCE!>foo<!>() + super.<!UNRESOLVED_REFERENCE!>foo<!>()) :
        <!INAPPLICABLE_CANDIDATE!>super<!>(x <!AMBIGUITY!>+<!> <!UNRESOLVED_REFERENCE!>foo<!>() + <!INSTANCE_ACCESS_BEFORE_SUPER_CALL!>this<!>.<!UNRESOLVED_REFERENCE!>foo<!>() + super.<!UNRESOLVED_REFERENCE!>foo<!>())
}
