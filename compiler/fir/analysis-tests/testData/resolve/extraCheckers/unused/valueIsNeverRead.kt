fun foo() {
    var a = <!VARIABLE_INITIALIZER_IS_REDUNDANT!>1<!>
    <!ASSIGNED_VALUE_IS_NEVER_READ!>a<!> = 2
    a = 3
    <!ASSIGNED_VALUE_IS_NEVER_READ!>a<!> += 1
}