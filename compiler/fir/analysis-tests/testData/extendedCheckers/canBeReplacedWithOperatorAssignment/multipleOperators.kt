fun foo() {
    var x = 0
    <!CAN_BE_VAL!>var<!> y = 0
    x <!CAN_BE_REPLACED_WITH_OPERATOR_ASSIGNMENT!>=<!> x + y + 5
}
