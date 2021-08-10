// !DIAGNOSTICS: -UNUSED_PARAMETER

fun A.foobar() = 3

class A {
    fun foo() = 1
    constructor(x: Any?)
    constructor() : this(object {
        fun bar() = <!UNRESOLVED_REFERENCE!>foo<!>() + this<!UNRESOLVED_LABEL!>@A<!>.foo() +
                    <!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>foobar<!>() + super<!UNRESOLVED_LABEL!>@A<!>.hashCode()
    })
}
