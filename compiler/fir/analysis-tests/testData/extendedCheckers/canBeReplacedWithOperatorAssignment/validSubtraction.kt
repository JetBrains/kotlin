fun foo() {
    var x = 0
    <!CAN_BE_REPLACED_WITH_OPERATOR_ASSIGNMENT!>x = x - 1<!>
}
