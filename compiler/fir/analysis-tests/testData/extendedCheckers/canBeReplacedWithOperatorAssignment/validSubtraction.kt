fun foo() {
    var x = 0
    x <!CAN_BE_REPLACED_WITH_OPERATOR_ASSIGNMENT!>=<!> x - 1
}
