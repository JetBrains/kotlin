fun foo() {
    var x = 0
    <!ASSIGNED_VALUE_IS_NEVER_READ!>x<!> = x / 1 + 1
}
