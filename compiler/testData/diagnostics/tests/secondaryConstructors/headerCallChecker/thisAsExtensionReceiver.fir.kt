// !DIAGNOSTICS: -UNUSED_PARAMETER

fun A.foobar() = 1
val A.prop: Int get() = 2

class A {
    constructor(x: Int)
    constructor() : <!INAPPLICABLE_CANDIDATE!>this<!>(
            <!UNRESOLVED_REFERENCE!>foobar<!>() +
            this.foobar() +
            <!UNRESOLVED_REFERENCE!>prop<!> +
            this.prop +
            this@A.prop
    )
}
