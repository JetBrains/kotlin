class A<T>(val a: T)
class B(val b: Any)
class C(val c: Any)

context(labelAInt@A<Int>, A<String>, labelB@B) fun f() {
    <!UNRESOLVED_LABEL!>this@labelAInt<!>.<!UNRESOLVED_REFERENCE!>a<!>.toFloat()
    <!UNRESOLVED_LABEL!>this@A<!>.<!UNRESOLVED_REFERENCE!>a<!>.length
    <!UNRESOLVED_LABEL!>this@labelB<!>.<!UNRESOLVED_REFERENCE!>b<!>
    <!UNRESOLVED_LABEL!>this@B<!>
}

context(labelAInt@A<Int>, A<String>, labelB@B) val C.p: Int
    get() {
        <!UNRESOLVED_LABEL!>this@labelAInt<!>.<!UNRESOLVED_REFERENCE!>a<!>.toFloat()
        <!UNRESOLVED_LABEL!>this@A<!>.<!UNRESOLVED_REFERENCE!>a<!>.length
        <!UNRESOLVED_LABEL!>this@B<!>
        <!UNRESOLVED_LABEL!>this@labelB<!>.<!UNRESOLVED_REFERENCE!>b<!>
        <!UNRESOLVED_LABEL!>this@C<!>.<!UNRESOLVED_REFERENCE!>c<!>
        this@p.c
        this.c
        return 1
    }