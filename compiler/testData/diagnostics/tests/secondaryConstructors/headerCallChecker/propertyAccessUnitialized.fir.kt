// !DIAGNOSTICS: -UNUSED_PARAMETER
open class B(x: Int)
class A : B {
    val prop = 1
    constructor(x: Int, y: Int = <!TYPE_MISMATCH!>x <!OVERLOAD_RESOLUTION_AMBIGUITY!>+<!> <!UNRESOLVED_REFERENCE!>prop<!> + <!INSTANCE_ACCESS_BEFORE_SUPER_CALL!>this<!>.prop<!>) :
        super(<!ARGUMENT_TYPE_MISMATCH!>x <!OVERLOAD_RESOLUTION_AMBIGUITY!>+<!> <!UNRESOLVED_REFERENCE!>prop<!> + <!INSTANCE_ACCESS_BEFORE_SUPER_CALL!>this<!>.prop<!>)
}
