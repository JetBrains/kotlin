fun foo() {
    var a = 0
    <!ASSIGNED_VALUE_IS_NEVER_READ!>a<!> += 10 + a
}