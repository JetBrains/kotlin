fun foo() {
    var a = 0
    <!ASSIGNED_VALUE_IS_NEVER_READ!>a<!> = (a + 1) / 2
}