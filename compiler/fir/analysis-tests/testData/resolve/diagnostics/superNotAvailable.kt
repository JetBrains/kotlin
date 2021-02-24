fun String.f() {
    <!SUPER_NOT_AVAILABLE!>super@f<!>.<!UNRESOLVED_REFERENCE!>compareTo<!>("")
    <!SUPER_NOT_AVAILABLE!>super<!>.<!UNRESOLVED_REFERENCE!>compareTo<!>("")
}

fun foo() {
    <!SUPER_NOT_AVAILABLE!>super<!>
    <!SUPER_NOT_AVAILABLE!>super<!>.<!UNRESOLVED_REFERENCE!>foo<!>()
    <!SUPER_NOT_AVAILABLE!>super<Nothing><!>.<!UNRESOLVED_REFERENCE!>foo<!>()
}

class A {
    fun act() {
        <!UNRESOLVED_REFERENCE!>println<!>("Test")
    }

    fun String.fact() {
        <!UNRESOLVED_REFERENCE!>println<!>("Fest")
    }
}
