fun foo() {
    <!NO_SUPERTYPE, NO_SUPERTYPE!>super<!>
    <!NO_SUPERTYPE, NO_SUPERTYPE!>super<!>.<!UNRESOLVED_REFERENCE!>foo<!>()
    super<Nothing>.<!UNRESOLVED_REFERENCE!>foo<!>()
}