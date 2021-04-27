// !DIAGNOSTICS: -UNUSED_PARAMETER

fun A.foobar() = 3

class A {
    fun foo() = 1
    constructor(x: () -> Int)
    constructor() : this(
            {
                <!ARGUMENT_TYPE_MISMATCH, TYPE_MISMATCH!><!UNRESOLVED_REFERENCE!>foo<!>() +
                <!INSTANCE_ACCESS_BEFORE_SUPER_CALL!>this<!>.<!UNRESOLVED_REFERENCE!>foo<!>() +
                <!UNRESOLVED_LABEL!>this@A<!>.<!UNRESOLVED_REFERENCE!>foo<!>() +
                <!UNRESOLVED_REFERENCE!>foobar<!>()<!>
            })
}
