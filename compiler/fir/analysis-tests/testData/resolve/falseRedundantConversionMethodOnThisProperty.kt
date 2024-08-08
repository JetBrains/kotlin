// WITH_EXPERIMENTAL_CHECKERS

class Test {
    var number: Int = 10
}

fun Test.foo() {
    val number = 20
    this.number <!CAN_BE_REPLACED_WITH_OPERATOR_ASSIGNMENT!>=<!> number + 1
}
