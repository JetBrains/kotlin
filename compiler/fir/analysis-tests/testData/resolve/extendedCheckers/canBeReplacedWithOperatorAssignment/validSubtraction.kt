fun foo() {
    var x = 0
    <!ASSIGNED_VALUE_IS_NEVER_READ!>x<!> <!CAN_BE_REPLACED_WITH_OPERATOR_ASSIGNMENT!>=<!> x - 1
}
