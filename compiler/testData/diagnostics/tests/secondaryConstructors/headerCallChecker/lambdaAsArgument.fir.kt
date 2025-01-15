// RUN_PIPELINE_TILL: FRONTEND
// DIAGNOSTICS: -UNUSED_PARAMETER

fun A.foobar() = 3

class A {
    fun foo() = 1
    constructor(x: () -> Int)
    constructor() : this(
            {
                <!RETURN_TYPE_MISMATCH, TYPE_MISMATCH!><!INSTANCE_ACCESS_BEFORE_SUPER_CALL!>foo<!>() +
                <!INSTANCE_ACCESS_BEFORE_SUPER_CALL!>this<!>.foo() +
                <!INSTANCE_ACCESS_BEFORE_SUPER_CALL!>this@A<!>.foo() +
                <!INSTANCE_ACCESS_BEFORE_SUPER_CALL!>foobar<!>()<!>
            })
}
