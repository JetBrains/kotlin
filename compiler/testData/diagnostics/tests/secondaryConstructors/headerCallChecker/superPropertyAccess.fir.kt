// !DIAGNOSTICS: -UNUSED_PARAMETER
open class B(val prop: Int)
class A : B {
    constructor(x: Int, y: Int = x <!AMBIGUITY!>+<!> <!UNRESOLVED_REFERENCE!>prop<!> + <!INSTANCE_ACCESS_BEFORE_SUPER_CALL!>this<!>.<!UNRESOLVED_REFERENCE!>prop<!> + super.<!UNRESOLVED_REFERENCE!>prop<!>) :
        super(<!ARGUMENT_TYPE_MISMATCH!>x <!AMBIGUITY!>+<!> <!UNRESOLVED_REFERENCE!>prop<!> + <!INSTANCE_ACCESS_BEFORE_SUPER_CALL!>this<!>.<!UNRESOLVED_REFERENCE!>prop<!> + super.<!UNRESOLVED_REFERENCE!>prop<!><!>)
}
