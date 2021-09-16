// !LANGUAGE: +ContextReceivers

class A {
    val x = 1
}

context(A) class B {
    val prop = x + this<!UNRESOLVED_REFERENCE!>@A<!>.<!DEBUG_INFO_MISSING_UNRESOLVED!>x<!>

    fun f() = x + this<!UNRESOLVED_REFERENCE!>@A<!>.<!DEBUG_INFO_MISSING_UNRESOLVED!>x<!>
}