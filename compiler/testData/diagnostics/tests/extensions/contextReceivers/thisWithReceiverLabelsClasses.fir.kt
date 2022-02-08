// !LANGUAGE: +ContextReceivers

class A {
    val x = 1
}

context(A) class B {
    val prop = <!UNRESOLVED_REFERENCE!>x<!> + this<!UNRESOLVED_LABEL!>@A<!>.x

    fun f() = <!UNRESOLVED_REFERENCE!>x<!> + this<!UNRESOLVED_LABEL!>@A<!>.x
}