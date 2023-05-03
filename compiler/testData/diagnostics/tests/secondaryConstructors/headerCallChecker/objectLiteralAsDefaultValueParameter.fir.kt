// !DIAGNOSTICS: -UNUSED_PARAMETER

fun A.foobar() = 3

class A {
    fun foo() = 1
    constructor( x: Any = object {
        fun bar() = <!INSTANCE_ACCESS_BEFORE_SUPER_CALL!>foo<!>() <!OVERLOAD_RESOLUTION_AMBIGUITY!>+<!> <!INSTANCE_ACCESS_BEFORE_SUPER_CALL!>this@A<!>.foo() +
                    <!INSTANCE_ACCESS_BEFORE_SUPER_CALL!>foobar<!>()
    })
}
