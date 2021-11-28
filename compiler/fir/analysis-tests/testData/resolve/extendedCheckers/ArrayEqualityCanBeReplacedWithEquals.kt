// WITH_STDLIB

fun foo(p: Int) {
    val a = arrayOf(1, 2, 3)
    val b = arrayOf(3, 2, 1)

    if (a <!ARRAY_EQUALITY_OPERATOR_CAN_BE_REPLACED_WITH_EQUALS!>==<!> b) { }
}

fun testsFromIdea() {
    val a = arrayOf("a")
    val b = a
    val c: Any? = null
    a <!ARRAY_EQUALITY_OPERATOR_CAN_BE_REPLACED_WITH_EQUALS!>==<!> b
    a == c
    a <!ARRAY_EQUALITY_OPERATOR_CAN_BE_REPLACED_WITH_EQUALS!>!=<!> b
}
