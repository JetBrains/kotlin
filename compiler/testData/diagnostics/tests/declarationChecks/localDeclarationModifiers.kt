package d

class T {
    fun baz() = 1
}

fun foo() {
    <!WRONG_MODIFIER_TARGET!>public<!> val <!UNUSED_VARIABLE!>i<!> = 11
    <!WRONG_MODIFIER_TARGET!>abstract<!> val <!VARIABLE_WITH_NO_TYPE_NO_INITIALIZER, UNUSED_VARIABLE!>j<!>
    <!WRONG_MODIFIER_TARGET!>override<!> fun T.baz() = 2
    <!WRONG_MODIFIER_TARGET!>private<!> fun bar() = 2
}