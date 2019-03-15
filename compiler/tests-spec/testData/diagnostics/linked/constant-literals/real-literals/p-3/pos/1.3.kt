/*
 * KOTLIN DIAGNOSTICS SPEC TEST (POSITIVE)
 *
 * SPEC VERSION: 0.1-draft
 * PLACE: constant-literals, real-literals -> paragraph 3 -> sentence 1
 * NUMBER: 3
 * DESCRIPTION: Real literals separeted by comments with omitted a whole-number part.
 */

// TESTCASE NUMBER: 1
val value_1 = /**/.99901

// TESTCASE NUMBER: 2
val value_2 = /** some doc */.1f

// TESTCASE NUMBER: 3
val value_3 = /** some doc *//**/.1

// TESTCASE NUMBER: 4
val value_4 = /** some /** some doc */ doc */.1e1

// TESTCASE NUMBER: 5
val value_5 = /**/
.1F

// TESTCASE NUMBER: 6
val value_6 = //0
.0
