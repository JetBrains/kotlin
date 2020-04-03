// !LANGUAGE: +NewInference
// !DIAGNOSTICS: -UNUSED_VARIABLE -ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE -UNUSED_VALUE -UNUSED_PARAMETER -UNUSED_EXPRESSION
// SKIP_TXT

/*
 * KOTLIN DIAGNOSTICS SPEC TEST (NEGATIVE)
 *
 * SPEC VERSION: 0.1-253
 * PLACE: statements, loop-statements, while-loop-statement -> paragraph 3 -> sentence 1
 * RELEVANT PLACES: statements, loop-statements, while-loop-statement -> paragraph 1 -> sentence 1
 * NUMBER: 1
 * DESCRIPTION: condition expression is not a subtype of kotlin.Boolean.
 * HELPERS: checkType
 */

// FILE: KotlinClass.kt
// TESTCASE NUMBER: 1
fun case1() {
    val condition: Any = true
    while (<!TYPE_MISMATCH!>condition<!> && <!TYPE_MISMATCH!>"true"<!>) {
    }
}

// TESTCASE NUMBER: 2
fun case2() {
    val condition: Boolean? = true
    while (<!TYPE_MISMATCH, TYPE_MISMATCH!>condition<!>) {
    }
}
