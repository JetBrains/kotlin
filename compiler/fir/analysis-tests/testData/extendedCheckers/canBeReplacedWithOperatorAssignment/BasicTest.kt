fun goo() {
    var a = 2
    val b = 4
    <!CAN_BE_REPLACED_WITH_OPERATOR_ASSIGNMENT!>a = a + 1 + b<!>
    <!CAN_BE_REPLACED_WITH_OPERATOR_ASSIGNMENT!>a = (a + 1)<!>
    a = a * b + 1
}
