class A() {
    override fun equals(other : Any?) : Boolean = false
}

fun f(): Unit {
    var x: Int? = 1
    x = null
    x + 1
    x <!UNSAFE_INFIX_CALL!>plus<!> 1
    x <!UNSAFE_OPERATOR_CALL!><<!> 1
    <!ASSIGNMENT_TYPE_MISMATCH!>x += 1<!>

    x == 1
    x != 1

    <!EQUALITY_NOT_APPLICABLE!>A() == 1<!>

    x === "1"
    x !== "1"

    x === 1
    x !== 1

    x..2
    x in 1..2

    val y : Boolean? = true
    false || <!CONDITION_TYPE_MISMATCH!>y<!>
    <!CONDITION_TYPE_MISMATCH!>y<!> && true
    <!CONDITION_TYPE_MISMATCH!>y<!> && <!CONDITION_TYPE_MISMATCH!>1<!>
}
