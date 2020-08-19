// !DIAGNOSTICS: -UNUSED_PARAMETER
class A {
    val prop = 1
    constructor(x: Int)
    constructor(x: Int, y: Int, z: Int = x <!AMBIGUITY!>+<!> <!UNRESOLVED_REFERENCE!>prop<!> + <!INSTANCE_ACCESS_BEFORE_SUPER_CALL!>this<!>.<!UNRESOLVED_REFERENCE!>prop<!>) :
        <!INAPPLICABLE_CANDIDATE!>this<!>(x <!AMBIGUITY!>+<!> <!UNRESOLVED_REFERENCE!>prop<!> + <!INSTANCE_ACCESS_BEFORE_SUPER_CALL!>this<!>.<!UNRESOLVED_REFERENCE!>prop<!>)
}
