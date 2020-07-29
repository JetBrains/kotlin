fun foo() {
    var x = 0
    x <!CAN_BE_REPLACED_WITH_OPERATOR_ASSIGNMENT!>=<!> x - 1 - 1

    x <!CAN_BE_REPLACED_WITH_OPERATOR_ASSIGNMENT!>=<!> x / 1
    x = 1 / x
    x <!CAN_BE_REPLACED_WITH_OPERATOR_ASSIGNMENT!>=<!> -1 + x
}
