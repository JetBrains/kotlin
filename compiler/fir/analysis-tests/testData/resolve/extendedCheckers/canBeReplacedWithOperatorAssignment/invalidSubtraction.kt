fun foo() {
    var x = 0
    <!ASSIGNED_VALUE_IS_NEVER_READ!>x<!> = 1 - x
}
