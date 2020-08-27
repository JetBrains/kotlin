fun foo() {
    var x = 0
    <!CAN_BE_VAL!>var<!> y = 0
    <!ASSIGNED_VALUE_IS_NEVER_READ!>x<!> <!CAN_BE_REPLACED_WITH_OPERATOR_ASSIGNMENT!>=<!> y + x + 5
}
