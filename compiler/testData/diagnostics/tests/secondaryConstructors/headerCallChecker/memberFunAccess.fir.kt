// !DIAGNOSTICS: -UNUSED_PARAMETER
class A {
    fun foo() = 1
    constructor(x: Int)
    constructor(x: Int, y: Int, z: Int = <!TYPE_MISMATCH!>x <!OVERLOAD_RESOLUTION_AMBIGUITY!>+<!> <!UNRESOLVED_REFERENCE!>foo<!>() + <!INSTANCE_ACCESS_BEFORE_SUPER_CALL!>this<!>.foo()<!>) :
        this(<!ARGUMENT_TYPE_MISMATCH!>x <!OVERLOAD_RESOLUTION_AMBIGUITY!>+<!> <!UNRESOLVED_REFERENCE!>foo<!>() + <!INSTANCE_ACCESS_BEFORE_SUPER_CALL!>this<!>.foo()<!>)
}
