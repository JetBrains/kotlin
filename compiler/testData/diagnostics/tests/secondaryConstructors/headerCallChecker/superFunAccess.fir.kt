// !DIAGNOSTICS: -UNUSED_PARAMETER
open class B(x: Int) {
    fun foo() = 1
}
class A : B {
    constructor(x: Int, y: Int = <!TYPE_MISMATCH!>x <!OVERLOAD_RESOLUTION_AMBIGUITY!>+<!> <!UNRESOLVED_REFERENCE!>foo<!>() + <!INSTANCE_ACCESS_BEFORE_SUPER_CALL!>this<!>.foo() + <!INSTANCE_ACCESS_BEFORE_SUPER_CALL!>super<!>.foo()<!>) :
        super(<!ARGUMENT_TYPE_MISMATCH!>x <!OVERLOAD_RESOLUTION_AMBIGUITY!>+<!> <!UNRESOLVED_REFERENCE!>foo<!>() + <!INSTANCE_ACCESS_BEFORE_SUPER_CALL!>this<!>.foo() + <!INSTANCE_ACCESS_BEFORE_SUPER_CALL!>super<!>.foo()<!>)
}
