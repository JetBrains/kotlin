// !DIAGNOSTICS: -UNUSED_PARAMETER
fun foo(x: Outer) = 1
class Outer {
    inner class Inner {
        val prop = 1
    }

    constructor(x: Int)
    constructor(x: Int, y: Int, z: Int = <!TYPE_MISMATCH!>x <!OVERLOAD_RESOLUTION_AMBIGUITY!>+<!> <!RESOLUTION_TO_CLASSIFIER!>Inner<!>().<!UNRESOLVED_REFERENCE!>prop<!> + <!INSTANCE_ACCESS_BEFORE_SUPER_CALL!>this<!>.Inner().prop<!>) :
        this(<!ARGUMENT_TYPE_MISMATCH!>x <!OVERLOAD_RESOLUTION_AMBIGUITY!>+<!> <!RESOLUTION_TO_CLASSIFIER!>Inner<!>().<!UNRESOLVED_REFERENCE!>prop<!> + <!INSTANCE_ACCESS_BEFORE_SUPER_CALL!>this<!>.Inner().prop<!>)
}
