fun foo() {
    <!SUPER_NOT_AVAILABLE!>super<!>
    <!SUPER_NOT_AVAILABLE!>super<!>.<!UNRESOLVED_REFERENCE!>foo<!>()
    <!SUPER_NOT_AVAILABLE!>super<Nothing><!>.<!UNRESOLVED_REFERENCE!>foo<!>()
}