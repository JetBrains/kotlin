class A<T>(val a: T)
class B(val b: Any)
class C(val c: Any)

context(A<Int>, A<String>, B) fun f() {
    <!UNRESOLVED_LABEL!>this@A<!>.<!UNRESOLVED_REFERENCE!>a<!>.length
    <!UNRESOLVED_LABEL!>this@B<!>.<!UNRESOLVED_REFERENCE!>b<!>
    <!NO_THIS!>this<!>
}

context(A<Int>, A<String>, B) fun C.f() {
    <!UNRESOLVED_LABEL!>this@A<!>.<!UNRESOLVED_REFERENCE!>a<!>.length
    <!UNRESOLVED_LABEL!>this@B<!>.<!UNRESOLVED_REFERENCE!>b<!>
    <!UNRESOLVED_LABEL!>this@C<!>.<!UNRESOLVED_REFERENCE!>c<!>
    this@f.c
    this.c
}