// FIR_IDENTICAL
// !DIAGNOSTICS: -UNUSED_PARAMETER

fun A.foobar() = 1
val A.prop: Int get() = 2

class A {
    constructor(x: Int)
    constructor() : this(
            <!INSTANCE_ACCESS_BEFORE_SUPER_CALL!>foobar<!>() +
            <!INSTANCE_ACCESS_BEFORE_SUPER_CALL!>this<!>.foobar() +
            <!INSTANCE_ACCESS_BEFORE_SUPER_CALL!>prop<!> +
            <!INSTANCE_ACCESS_BEFORE_SUPER_CALL!>this<!>.prop +
            <!INSTANCE_ACCESS_BEFORE_SUPER_CALL!>this@A<!>.prop
    )
}
