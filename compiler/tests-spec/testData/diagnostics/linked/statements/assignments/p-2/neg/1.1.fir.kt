// !LANGUAGE: +NewInference
// !DIAGNOSTICS: -UNUSED_VARIABLE -ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE -UNUSED_VALUE -UNUSED_PARAMETER -UNUSED_EXPRESSION
// SKIP_TXT

/*
 * TESTCASE NUMBER: 1
 * NOTE: an identifier referring to a unmutable property
 */
fun case1() {
    val x : Any
    x = "0"
    x = 1
    x = 2.0

    val y : Any = 0
    y = "0"
    y = 1.0
}

/*
 * TESTCASE NUMBER: 2
 * NOTE: an identifier referring to a unmutable property
 */
fun case2() {
    val x : Any
    mutableListOf(0).forEach({ x = it })

    val y : Any = 1
    mutableListOf(1).forEach({ y = it })
}
