// RUN_PIPELINE_TILL: FRONTEND
// FIR_IDENTICAL
// DIAGNOSTICS: -UNUSED_PARAMETER

fun A.foobar() = 3

class A {
    fun foo() = 1
    constructor( x: Any = object {
        fun bar() = <!INSTANCE_ACCESS_BEFORE_SUPER_CALL!>foo<!>() + <!INSTANCE_ACCESS_BEFORE_SUPER_CALL!>this@A<!>.foo() +
                    <!INSTANCE_ACCESS_BEFORE_SUPER_CALL!>foobar<!>()
    })
}
