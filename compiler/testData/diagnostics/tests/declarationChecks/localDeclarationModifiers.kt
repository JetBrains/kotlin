package d

class T {
    fun baz() = 1
}

fun foo() {
    <!ILLEGAL_MODIFIER!>public<!> val <!UNUSED_VARIABLE!>i<!> = 11
    <!ILLEGAL_MODIFIER!>abstract<!> val <!VARIABLE_WITH_NO_TYPE_NO_INITIALIZER, UNUSED_VARIABLE!>j<!>
    <!ILLEGAL_MODIFIER!>override<!> fun T.baz() = 2
    <!ILLEGAL_MODIFIER!>private<!> fun bar() = 2
}