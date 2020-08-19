// !DIAGNOSTICS: -UNUSED_PARAMETER
open class B(x: Int)
class A : B {
    val prop = 1
    constructor(x: Int, y: Int = x <!AMBIGUITY!>+<!> <!UNRESOLVED_REFERENCE!>prop<!> + <!INSTANCE_ACCESS_BEFORE_SUPER_CALL!>this<!>.<!UNRESOLVED_REFERENCE!>prop<!>) :
        <!INAPPLICABLE_CANDIDATE!>super<!>(x <!AMBIGUITY!>+<!> <!UNRESOLVED_REFERENCE!>prop<!> + <!INSTANCE_ACCESS_BEFORE_SUPER_CALL!>this<!>.<!UNRESOLVED_REFERENCE!>prop<!>)
}
