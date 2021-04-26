// !DIAGNOSTICS: -UNUSED_PARAMETER

fun A.foobar() = 1
val A.prop: Int get() = 2

class A {
    constructor(x: Int)
    constructor() : this(
            <!ARGUMENT_TYPE_MISMATCH!><!UNRESOLVED_REFERENCE!>foobar<!>() +
            <!INSTANCE_ACCESS_BEFORE_SUPER_CALL!>this<!>.foobar() +
            <!UNRESOLVED_REFERENCE!>prop<!> +
            <!INSTANCE_ACCESS_BEFORE_SUPER_CALL!>this<!>.prop +
            <!UNRESOLVED_LABEL!>this@A<!>.prop<!>
    )
}
