// !DIAGNOSTICS: -UNUSED_VARIABLE -ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE -UNUSED_VALUE -UNUSED_PARAMETER -UNUSED_EXPRESSION
// SKIP_TXT

/*
 * KOTLIN DIAGNOSTICS SPEC TEST (NEGATIVE)
 *
 * SPEC VERSION: 0.1-222
 * MAIN LINK: statements, assignments -> paragraph 2 -> sentence 1
 * PRIMARY LINKS: statements, assignments -> paragraph 3 -> sentence 1
 * statements, assignments, simple-assignments -> paragraph 1 -> sentence 2
 * NUMBER: 1
 * DESCRIPTION: Check the expression is not assignable if an identifier referring to an unmutable property
 */

/*
 * TESTCASE NUMBER: 1
 * NOTE: an identifier referring to a unmutable property
 */
fun case1() {
    val x : Any
    x = "0"
    <!VAL_REASSIGNMENT!>x<!> = 1
    <!VAL_REASSIGNMENT!>x<!> = 2.0

    val y : Any = 0
    <!VAL_REASSIGNMENT!>y<!> = "0"
    <!VAL_REASSIGNMENT!>y<!> = 1.0
}

/*
 * TESTCASE NUMBER: 2
 * NOTE: an identifier referring to a unmutable property
 */
fun case2() {
    val x : Any
    mutableListOf(0).forEach({ <!VAL_REASSIGNMENT!>x<!> = it })

    val y : Any = 1
    mutableListOf(1).forEach({ <!VAL_REASSIGNMENT!>y<!> = it })
}
