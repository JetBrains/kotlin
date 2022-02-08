// !LANGUAGE: +ContextReceivers

class A(val a: String?)

context(A) fun f() {
    if (this<!UNRESOLVED_LABEL!>@A<!>.a == null) return
    this<!UNRESOLVED_LABEL!>@A<!>.a.length
}