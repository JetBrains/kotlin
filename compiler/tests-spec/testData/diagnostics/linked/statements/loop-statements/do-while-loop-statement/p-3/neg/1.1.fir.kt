// !DIAGNOSTICS: -UNUSED_VARIABLE -ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE -UNUSED_VALUE -UNUSED_PARAMETER -UNUSED_EXPRESSION
// SKIP_TXT

/*
 * KOTLIN DIAGNOSTICS SPEC TEST (NEGATIVE)
 *
 * SPEC VERSION: 0.1-253
 * MAIN LINK: statements, loop-statements, do-while-loop-statement -> paragraph 3 -> sentence 1
 * PRIMARY LINKS: statements, loop-statements, do-while-loop-statement -> paragraph 1 -> sentence 1
 * NUMBER: 1
 * DESCRIPTION: condition expression is not a subtype of kotlin.Boolean.
 */

// TESTCASE NUMBER: 1
fun case1() {
    do {
    } while (<!CONDITION_TYPE_MISMATCH!>"boo"<!>)
}

// TESTCASE NUMBER: 2
fun case2() {
    val condition: Any = true
    do {
    } while (<!CONDITION_TYPE_MISMATCH!>condition<!>)
}

// TESTCASE NUMBER: 3
fun case3() {
    val condition: Boolean? = true
    do {
    } while (<!CONDITION_TYPE_MISMATCH!>condition<!>)
}
