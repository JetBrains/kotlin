// !DIAGNOSTICS: -UNUSED_PARAMETER
class A {
    val prop = 1
    constructor(x: Int)
    constructor(x: Int, y: Int, z: Int = <!TYPE_MISMATCH!>x <!OVERLOAD_RESOLUTION_AMBIGUITY!>+<!> <!UNRESOLVED_REFERENCE!>prop<!> + <!INSTANCE_ACCESS_BEFORE_SUPER_CALL!>this<!>.prop<!>) :
        this(<!ARGUMENT_TYPE_MISMATCH!>x <!OVERLOAD_RESOLUTION_AMBIGUITY!>+<!> <!UNRESOLVED_REFERENCE!>prop<!> + <!INSTANCE_ACCESS_BEFORE_SUPER_CALL!>this<!>.prop<!>)
}
