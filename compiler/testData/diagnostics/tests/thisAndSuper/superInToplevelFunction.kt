fun foo() {
    <!SUPER_IS_NOT_AN_EXPRESSION!>super<!>
    <!SUPER_NOT_AVAILABLE!>super<!>.<!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE!>foo<!>()
    <!SUPER_NOT_AVAILABLE!>super<Nothing><!>.<!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE!>foo<!>()
}