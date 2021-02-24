fun foo() {
    <!VARIABLE_NEVER_READ{LT}!>var <!VARIABLE_NEVER_READ{PSI}!>x<!> = 0<!>
    val y = 0
    val z = 0
    <!ASSIGNED_VALUE_IS_NEVER_READ!>x<!> = y + z
}
