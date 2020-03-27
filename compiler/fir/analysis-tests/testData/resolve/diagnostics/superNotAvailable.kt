fun String.f() {
    <!NO_SUPERTYPE, NO_SUPERTYPE, SUPER_NOT_AVAILABLE!>super@f<!>.<!UNRESOLVED_REFERENCE!>compareTo<!>("")
    <!NO_SUPERTYPE, NO_SUPERTYPE, SUPER_NOT_AVAILABLE!>super<!>.<!UNRESOLVED_REFERENCE!>compareTo<!>("")
}

fun foo() {
    <!NO_SUPERTYPE, NO_SUPERTYPE, SUPER_NOT_AVAILABLE!>super<!>
    <!NO_SUPERTYPE, NO_SUPERTYPE, SUPER_NOT_AVAILABLE!>super<!>.<!UNRESOLVED_REFERENCE!>foo<!>()
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