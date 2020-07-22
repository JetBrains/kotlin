fun foo() {
    var x = 0
    var y = 0
    <!CAN_BE_REPLACED_WITH_OPERATOR_ASSIGNMENT!>x = x + y + 5<!>
}
