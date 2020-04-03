// !DIAGNOSTICS: -UNUSED_PARAMETER

fun A.foobar() = 3

class A {
    fun foo() = 1
    constructor(x: () -> Int)
    constructor() : this(
            {
                <!UNRESOLVED_REFERENCE!>foo<!>() +
                this.<!UNRESOLVED_REFERENCE!>foo<!>() +
                this@A.<!UNRESOLVED_REFERENCE!>foo<!>() +
                <!UNRESOLVED_REFERENCE!>foobar<!>()
            })
}
