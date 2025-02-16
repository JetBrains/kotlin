// RUN_PIPELINE_TILL: BACKEND
// WITH_EXTRA_CHECKERS
// ISSUE: KT-73352

fun normal() {
    val a = arrayOf('a', 'b', 'c')
    val b = arrayOf('a', 'b', 'c')
    a <!ARRAY_EQUALITY_OPERATOR_CAN_BE_REPLACED_WITH_EQUALS!>==<!> b
}

fun chars() {
    val a = charArrayOf('a', 'b', 'c')
    val b = charArrayOf('a', 'b', 'c')
    a <!ARRAY_EQUALITY_OPERATOR_CAN_BE_REPLACED_WITH_EQUALS!>==<!> b
}

fun normalAndChar() {
    val a = arrayOf('a', 'b', 'c')
    val b = charArrayOf('a', 'b', 'c')
    a == b
}

fun charAndNormal() {
    val a = arrayOf('a', 'b', 'c')
    val b = charArrayOf('a', 'b', 'c')
    a == b
}
