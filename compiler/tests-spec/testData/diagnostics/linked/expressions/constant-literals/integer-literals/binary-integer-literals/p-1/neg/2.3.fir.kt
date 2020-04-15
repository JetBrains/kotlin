/*
 * KOTLIN DIAGNOSTICS SPEC TEST (NEGATIVE)
 *
 * SPEC VERSION: 0.1-100
 * PLACE: expressions, constant-literals, integer-literals, binary-integer-literals -> paragraph 1 -> sentence 2
 * NUMBER: 3
 * DESCRIPTION: Binary integer literals with an underscore in the last position.
 */

// TESTCASE NUMBER: 1
val value_1 = 0b0_1_1_0_1_1_____

// TESTCASE NUMBER: 2
val value_2 = 0B1_______1_______0_______1_

// TESTCASE NUMBER: 3
val value_3 = 0B000000000_

// TESTCASE NUMBER: 4
val value_4 = <!ILLEGAL_CONST_EXPRESSION!>0b_<!>

// TESTCASE NUMBER: 5
val value_5 = <!ILLEGAL_CONST_EXPRESSION!>0B______________<!>

// TESTCASE NUMBER: 6
val value_6 = 0B0_

// TESTCASE NUMBER: 7
val value_7 = 0B10_
