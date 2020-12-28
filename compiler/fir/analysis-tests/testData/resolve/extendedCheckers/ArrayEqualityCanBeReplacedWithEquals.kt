// WITH_RUNTIME

fun foo(p: Int) {
    val a = arrayOf(1, 2, 3)
    val b = arrayOf(3, 2, 1)

    if (<!ARRAY_EQUALITY_OPERATOR_CAN_BE_REPLACED_WITH_EQUALS{LT}!>a <!ARRAY_EQUALITY_OPERATOR_CAN_BE_REPLACED_WITH_EQUALS{PSI}!>==<!> b<!>) { }
}

fun testsFromIdea() {
    val a = arrayOf("a")
    val b = a
    val c: Any? = null
    <!ARRAY_EQUALITY_OPERATOR_CAN_BE_REPLACED_WITH_EQUALS{LT}!>a <!ARRAY_EQUALITY_OPERATOR_CAN_BE_REPLACED_WITH_EQUALS{PSI}!>==<!> b<!>
    a == c
    <!ARRAY_EQUALITY_OPERATOR_CAN_BE_REPLACED_WITH_EQUALS{LT}!>a <!ARRAY_EQUALITY_OPERATOR_CAN_BE_REPLACED_WITH_EQUALS{PSI}!>!=<!> b<!>
}
