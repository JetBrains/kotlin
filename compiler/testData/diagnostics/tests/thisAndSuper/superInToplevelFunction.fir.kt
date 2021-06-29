fun foo() {
    <!SUPER_IS_NOT_AN_EXPRESSION!>super<!>
    <!SUPER_NOT_AVAILABLE!>super<!>.foo()
    <!SUPER_NOT_AVAILABLE!>super<Nothing><!>.foo()
}
