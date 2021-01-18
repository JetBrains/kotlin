class A<T>(val a: T)
class B(val b: Any)
class C(val c: Any)

context(A<Int>, A<String>, B) var p: Int
    get() {
        <!UNRESOLVED_LABEL!>this@A<!>.<!UNRESOLVED_REFERENCE!>a<!>.<!UNRESOLVED_REFERENCE!>toDouble<!>()
        <!UNRESOLVED_LABEL!>this@A<!>.<!UNRESOLVED_REFERENCE!>a<!>.<!UNRESOLVED_REFERENCE!>length<!>
        <!UNRESOLVED_LABEL!>this@B<!>.<!UNRESOLVED_REFERENCE!>b<!>
        <!NO_THIS!>this<!>
        return 1
    }
    set(value) {
        <!UNRESOLVED_LABEL!>this@A<!>.<!UNRESOLVED_REFERENCE!>a<!>.<!UNRESOLVED_REFERENCE!>length<!>
        <!UNRESOLVED_LABEL!>this@B<!>.<!UNRESOLVED_REFERENCE!>b<!>
        <!NO_THIS!>this<!>
        field = value
    }

context(A<Int>, A<String>, B) val C.p: Int
    get() {
        <!UNRESOLVED_LABEL!>this@A<!>.<!UNRESOLVED_REFERENCE!>a<!>.<!UNRESOLVED_REFERENCE!>length<!>
        <!UNRESOLVED_LABEL!>this@B<!>.<!UNRESOLVED_REFERENCE!>b<!>
        <!UNRESOLVED_LABEL!>this@C<!>.<!UNRESOLVED_REFERENCE!>c<!>
        this@p.c
        this.c
        return 1
    }