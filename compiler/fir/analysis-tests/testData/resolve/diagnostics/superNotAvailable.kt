fun String.f() {
    <!SUPER_NOT_AVAILABLE!>super@f<!>.<!UNRESOLVED_REFERENCE{LT}!><!UNRESOLVED_REFERENCE{PSI}!>compareTo<!>("")<!>
    <!SUPER_NOT_AVAILABLE!>super<!>.<!UNRESOLVED_REFERENCE{LT}!><!UNRESOLVED_REFERENCE{PSI}!>compareTo<!>("")<!>
}

fun foo() {
    <!SUPER_NOT_AVAILABLE!>super<!>
    <!SUPER_NOT_AVAILABLE!>super<!>.<!UNRESOLVED_REFERENCE{LT}!><!UNRESOLVED_REFERENCE{PSI}!>foo<!>()<!>
    <!SUPER_NOT_AVAILABLE!>super<Nothing><!>.<!UNRESOLVED_REFERENCE{LT}!><!UNRESOLVED_REFERENCE{PSI}!>foo<!>()<!>
}

class A {
    fun act() {
        <!UNRESOLVED_REFERENCE{LT}!><!UNRESOLVED_REFERENCE{PSI}!>println<!>("Test")<!>
    }

    fun String.fact() {
        <!UNRESOLVED_REFERENCE{LT}!><!UNRESOLVED_REFERENCE{PSI}!>println<!>("Fest")<!>
    }
}
